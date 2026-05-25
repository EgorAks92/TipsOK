package com.chaiok.pos.data.ecr

import android.util.Log
import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcPaymentResponse
import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcEcrCommand
import com.chaiok.pos.domain.repository.PcPaymentCommandRepository
import com.chaiok.pos.domain.repository.SettingsRepository
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class XchengPcPaymentCommandRepository(
    private val client: XchengWireEcrPortClient,
    private val settingsRepository: SettingsRepository,
    context: Context
) : PcPaymentCommandRepository {

    private val rawLogger = Arcus2RawFrameLogger(context)

    private enum class PcEcrLifecycleState {
        Disconnected,
        Listening,
        PausedForPayment,
        Stopped,
        Error
    }

    private val commands = MutableSharedFlow<PcPaymentCommand>(extraBufferCapacity = 1)
    private val status = MutableStateFlow<PcUsbConnectionStatus>(PcUsbConnectionStatus.Idle)
    private val lifecycleMutex = Mutex()

    @Volatile
    private var lifecycleState: PcEcrLifecycleState = PcEcrLifecycleState.Disconnected

    override fun observeCommands(): Flow<PcPaymentCommand> = commands

    override fun observeStatus(): Flow<PcUsbConnectionStatus> = status

    override suspend fun sendResponse(response: PcPaymentResponse): Result<Unit> =
        client.send(response.payload.toByteArray(Charsets.UTF_8))

    /**
     * Sends final payment_result frame back to PC/ECR.
     *
     * Important:
     * - Do not call client.sendPaymentResult(frame) here.
     *   Some previous patches may leave duplicate overloads in XchengWireEcrPortClient,
     *   which causes overload resolution ambiguity.
     * - Encode frame here and send raw bytes through client.send(bytes).
     * - Resume/open transport before sending because it may have been closed by pauseForPayment().
     * - Do not move repository lifecycle to Listening here. ViewModel calls resumeAfterPayment()
     *   after result sending, and that method owns the final lifecycle transition.
     */
    override suspend fun sendPaymentResult(frame: ChaiOkEcrPaymentResultFrame): Result<Unit> =
        lifecycleMutex.withLock {
            if (lifecycleState == PcEcrLifecycleState.Stopped) {
                Log.w(
                    TAG,
                    "ECR repository was stopped before payment_result; try to recover " +
                            "commandId=${frame.commandId} status=${frame.status}"
                )

                lifecycleState = PcEcrLifecycleState.Disconnected
                status.value = PcUsbConnectionStatus.BindingService
            }

            Log.i(
                TAG,
                "Prepare ECR transport for payment_result commandId=${frame.commandId} status=${frame.status}"
            )

            val resumeResult = client.resumeTransportAfterPayment()
            if (resumeResult.isFailure) {
                val message = resumeResult.exceptionOrNull()?.message ?: "resume transport error"
                lifecycleState = PcEcrLifecycleState.Error
                status.value = PcUsbConnectionStatus.Error(message)

                Log.e(
                    TAG,
                    "ECR transport resume before payment_result failed " +
                            "commandId=${frame.commandId}: $message",
                    resumeResult.exceptionOrNull()
                )

                return@withLock resumeResult
            }

            val bytes = ChaiOkEcrFrameEncoder.encodePaymentResultLine(frame)

            Log.i(
                TAG,
                "send payment_result commandId=${frame.commandId} " +
                        "orderId=${frame.orderId ?: "-"} " +
                        "status=${frame.status} " +
                        "success=${frame.success} " +
                        "bytes=${bytes.size}"
            )

            val sendResult = client.send(bytes)

            if (sendResult.isSuccess) {
                lifecycleState = PcEcrLifecycleState.Listening
                status.value = PcUsbConnectionStatus.WaitingForData

                Log.i(
                    TAG,
                    "payment_result sent commandId=${frame.commandId} status=${frame.status}"
                )
            } else {
                val message = sendResult.exceptionOrNull()?.message ?: "send payment_result error"
                lifecycleState = PcEcrLifecycleState.Error
                status.value = PcUsbConnectionStatus.Error(message)

                Log.e(
                    TAG,
                    "payment_result send failed commandId=${frame.commandId} " +
                            "status=${frame.status}: $message",
                    sendResult.exceptionOrNull()
                )
            }

            sendResult
        }

    override suspend fun sendArcus2PaymentResult(sourceCommand: PcPaymentCommand, result: PcEcrFinalPaymentResult, receiptText: String?, settings: Arcus2NewWaySettings): Result<Unit> = lifecycleMutex.withLock {
        val session = Arcus2CashRegisterSession(client, rawLogger, settings)
        fun status(text: String) = if (settings.sendStatusMessages) session.sendCommandAndWaitOk("STATUS:$text") else Result.success(Unit)
        fun printIfNeeded() : Result<Unit> {
            if (!settings.sendPrintCommands || receiptText.isNullOrBlank()) return Result.success(Unit)
            if (settings.sendStartEndPrint) session.sendCommandAndWaitOk("STARTPRINT:CUSTOMER").getOrElse { return Result.failure(it) }
            session.sendPrintReceipt(receiptText).getOrElse { return Result.failure(it) }
            if (settings.sendStartEndPrint) session.sendCommandAndWaitOk("ENDPRINT:CUSTOMER").getOrElse { return Result.failure(it) }
            return Result.success(Unit)
        }
        when (result) {
            is PcEcrFinalPaymentResult.Approved -> { status("Одобрено").getOrElse { return@withLock Result.failure(it) }; printIfNeeded().getOrElse { return@withLock Result.failure(it) }; session.sendCommandAndWaitOk("STORERC:00").getOrElse { return@withLock Result.failure(it) } }
            is PcEcrFinalPaymentResult.Declined -> { status("Отклонено").getOrElse { return@withLock Result.failure(it) }; printIfNeeded().getOrElse { return@withLock Result.failure(it) }; session.sendCommandAndWaitOk("STORERC:${result.resultCode ?: settings.declinedDefaultRc}").getOrElse { return@withLock Result.failure(it) } }
            is PcEcrFinalPaymentResult.Cancelled -> { status("Отменено").getOrElse { return@withLock Result.failure(it) }; session.sendCommandAndWaitOk("STORERC:${settings.cancelledRc}").getOrElse { return@withLock Result.failure(it) } }
            is PcEcrFinalPaymentResult.Error -> { status("Ошибка").getOrElse { return@withLock Result.failure(it) }; session.sendCommandAndWaitOk("STORERC:${settings.errorRc}").getOrElse { return@withLock Result.failure(it) } }
        }
        if (settings.sendSetTags) session.sendSetTags(Arcus2TagsBuilder.buildPaymentTags(result, sourceCommand.amount, sourceCommand.currency, null)).getOrElse { return@withLock Result.failure(it) }
        session.sendCommandAndWaitOk("ENDTR").getOrElse { return@withLock Result.failure(it) }
        lifecycleState = PcEcrLifecycleState.Listening
        status.value = PcUsbConnectionStatus.WaitingForData
        Result.success(Unit)
    }

    override suspend fun listenOnce() {
        val ensureResult = lifecycleMutex.withLock {
            if (lifecycleState == PcEcrLifecycleState.PausedForPayment) {
                Log.i(TAG, "Ignore ECR command while payment is active")
                return
            }

            if (lifecycleState == PcEcrLifecycleState.Stopped) {
                Log.i(TAG, "ECR listen requested after stopped; restart session")
                lifecycleState = PcEcrLifecycleState.Disconnected
            }

            Log.i(TAG, "ECR session ensure connected")
            status.value = PcUsbConnectionStatus.BindingService

            client.ensureTransportReady().also { result ->
                if (result.isFailure) {
                    val message = result.exceptionOrNull()?.message ?: "connect error"
                    Log.e(TAG, "ensure transport ready failed: $message", result.exceptionOrNull())
                    lifecycleState = PcEcrLifecycleState.Error
                    status.value = PcUsbConnectionStatus.Error(message)
                } else {
                    lifecycleState = PcEcrLifecycleState.Listening
                    status.value = PcUsbConnectionStatus.WaitingForData
                    Log.i(TAG, "ECR listening started")
                }
            }
        }

        if (ensureResult.isFailure) {
            delay(ERROR_VISIBLE_DELAY_MS)
            return
        }

        val received = client.receiveOnce()
        if (received.isFailure) {
            val wasPausedOrStopped = lifecycleMutex.withLock {
                lifecycleState == PcEcrLifecycleState.PausedForPayment ||
                        lifecycleState == PcEcrLifecycleState.Stopped
            }

            if (wasPausedOrStopped) {
                Log.i(TAG, "Ignore receive failure because ECR is paused/stopped")
                return
            }

            val message = received.exceptionOrNull()?.message ?: "receive error"
            Log.e(TAG, "receive failed: $message", received.exceptionOrNull())

            lifecycleMutex.withLock {
                lifecycleState = PcEcrLifecycleState.Error
                status.value = PcUsbConnectionStatus.Error(message)
            }

            delay(ERROR_VISIBLE_DELAY_MS)
            return
        }

        val bytes = received.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            delay(LISTEN_LOOP_DELAY_MS)
            return
        }

        val canEmit = lifecycleMutex.withLock {
            lifecycleState != PcEcrLifecycleState.PausedForPayment &&
                    lifecycleState != PcEcrLifecycleState.Stopped
        }

        if (!canEmit) {
            Log.i(TAG, "Ignore ECR command while payment is active")
            return
        }

        Log.i(TAG, "recv hex=${bytes.toHexPreview()}")

        val settings = settingsRepository.observeSettings().first()
        val command = when (settings.pcEcrProtocol) {
            PcEcrProtocol.CHAIOK_JSON -> PcPaymentCommandParser.parse(bytes)
            PcEcrProtocol.ARCUS2_NEWWAY -> {
                val adapter = Arcus2NewWayProtocolAdapter({ settings.arcus2NewWaySettings }, rawLogger)
                when (val parsed = adapter.parseIncoming(bytes)) {
                    is EcrParseResult.Command -> when (val cmd = parsed.command) {
                        is PcEcrCommand.Payment -> PcPaymentCommand(amount = cmd.amount, commandId = cmd.commandId, orderId = cmd.orderId, currency = cmd.currency, rawPayloadPreview = "arcus2", sourceProtocol = PcEcrProtocol.ARCUS2_NEWWAY)
                        is PcEcrCommand.Ping -> { sendArcus2PaymentResult(PcPaymentCommand(amount = java.math.BigDecimal.ONE, sourceProtocol = PcEcrProtocol.ARCUS2_NEWWAY), PcEcrFinalPaymentResult.Approved(), null, settings.arcus2NewWaySettings); null }
                        is PcEcrCommand.Refund, is PcEcrCommand.Reversal, is PcEcrCommand.Settlement -> { sendArcus2PaymentResult(PcPaymentCommand(amount = java.math.BigDecimal.ONE, sourceProtocol = PcEcrProtocol.ARCUS2_NEWWAY), PcEcrFinalPaymentResult.Error("unsupported"), null, settings.arcus2NewWaySettings); null }
                        else -> null
                    }
                    else -> null
                }
            }
        }
        if (command != null) {
            Log.i(
                TAG,
                "ECR command received commandId=${command.commandId ?: "-"} currency=${command.currency}"
            )
            commands.emit(command)
        } else {
            Log.w(TAG, "payload received but parser returned null. hex=${bytes.toHexPreview()}")
        }
    }

    override suspend fun pauseForPayment(): Result<Unit> =
        lifecycleMutex.withLock {
            Log.i(TAG, "ECR pause for SSP payment")

            if (lifecycleState == PcEcrLifecycleState.PausedForPayment) {
                return@withLock Result.success(Unit)
            }

            lifecycleState = PcEcrLifecycleState.PausedForPayment
            status.value = PcUsbConnectionStatus.Idle

            val result = client.pauseTransportForPayment()

            if (result.isSuccess) {
                Log.i(TAG, "ECR paused for SSP payment")
            } else {
                Log.w(
                    TAG,
                    "ECR pause transport failed but continue as paused",
                    result.exceptionOrNull()
                )
            }

            Result.success(Unit)
        }

    override suspend fun resumeAfterPayment(): Result<Unit> =
        lifecycleMutex.withLock {
            Log.i(TAG, "ECR resume after SSP payment")

            if (lifecycleState == PcEcrLifecycleState.Stopped) {
                Log.i(TAG, "Skip ECR resume: repository stopped completely")
                return@withLock Result.success(Unit)
            }

            val result = client.resumeTransportAfterPayment()

            if (result.isSuccess) {
                lifecycleState = PcEcrLifecycleState.Listening
                status.value = PcUsbConnectionStatus.WaitingForData
                Log.i(TAG, "ECR resumed and listening")
            } else {
                val message = result.exceptionOrNull()?.message ?: "resume error"
                lifecycleState = PcEcrLifecycleState.Error
                status.value = PcUsbConnectionStatus.Error(message)
                Log.e(TAG, "ECR resume after payment failed: $message", result.exceptionOrNull())
            }

            result
        }

    override suspend fun stopCompletely(): Result<Unit> =
        lifecycleMutex.withLock {
            Log.i(TAG, "ECR stop completely")

            if (
                lifecycleState == PcEcrLifecycleState.Stopped ||
                lifecycleState == PcEcrLifecycleState.Disconnected
            ) {
                return@withLock Result.success(Unit)
            }

            val result = client.closeCompletely()

            if (result.isSuccess) {
                lifecycleState = PcEcrLifecycleState.Stopped
                status.value = PcUsbConnectionStatus.Idle
            } else {
                lifecycleState = PcEcrLifecycleState.Error
                Log.e(TAG, "ECR stop completely failed", result.exceptionOrNull())
            }

            result
        }

    private fun ByteArray.toHexPreview(limit: Int = 96): String =
        take(limit).joinToString(" ") { "%02X".format(it) }

    private companion object {
        private const val TAG = "PcUsbEcrFlow"
        private const val LISTEN_LOOP_DELAY_MS = 300L
        private const val ERROR_VISIBLE_DELAY_MS = 2_500L
    }
}
package com.chaiok.pos.data.ecr

import android.util.Log
import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcPaymentResponse
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.repository.PcPaymentCommandRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class XchengPcPaymentCommandRepository(
    private val client: XchengWireEcrPortClient
) : PcPaymentCommandRepository {

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

        val command = PcPaymentCommandParser.parse(bytes)
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
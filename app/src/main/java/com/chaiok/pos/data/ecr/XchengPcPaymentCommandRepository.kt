package com.chaiok.pos.data.ecr

import android.util.Log
import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcPaymentResponse
import com.chaiok.pos.domain.model.PcEcrOperationType
import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcEcrCommand
import java.math.BigDecimal
import java.math.RoundingMode
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
    private data class Arcus2AdditionalData(
        val rrn: String? = null,
        val amount: BigDecimal? = null,
        val currency: String? = null,
        val orderId: String? = null,
        val rawTags: Map<String, String> = emptyMap()
    )

    // TODO: wire enableRawArcus2Log from AppSettings into logger creation dynamically
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

    @Volatile
    private var activeArcus2Transaction: Boolean = false

    @Volatile
    private var activeArcus2CommandId: String? = null

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

    override suspend fun sendArcus2PaymentResult(sourceCommand: PcPaymentCommand, result: PcEcrFinalPaymentResult, receiptText: String?, settings: Arcus2NewWaySettings, terminalId: String?): Result<Unit> {
        lifecycleMutex.withLock {
            if (lifecycleState == PcEcrLifecycleState.Stopped && !activeArcus2Transaction) {
                val message = "ARCUS2 result cannot be sent: repository stopped and COM session is lost"
                lifecycleState = PcEcrLifecycleState.Error
                status.value = PcUsbConnectionStatus.Error(message)
                return Result.failure(IllegalStateException(message))
            }

            if (lifecycleState == PcEcrLifecycleState.Stopped && activeArcus2Transaction) {
                Log.w(
                    TAG,
                    "ARCUS2 result requested while lifecycle=Stopped but activeArcus2Transaction=true; sending final result on kept transport commandId=${activeArcus2CommandId ?: "-"}"
                )
                lifecycleState = PcEcrLifecycleState.PausedForPayment
                status.value = PcUsbConnectionStatus.Idle
            }
        }

        val session = Arcus2CashRegisterSession(client, rawLogger, settings)
        val sequence = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(sourceCommand, result, receiptText, settings, terminalId)
        val resultStatus = when (result) {
            is PcEcrFinalPaymentResult.Approved -> "approved"
            is PcEcrFinalPaymentResult.Declined -> "declined"
            is PcEcrFinalPaymentResult.Cancelled -> "cancelled"
            is PcEcrFinalPaymentResult.Error -> "error"
        }
        Log.i(TAG, "ARCUS2 final result send start commandId=${sourceCommand.commandId ?: "-"} lifecycle=$lifecycleState active=$activeArcus2Transaction status=$resultStatus minimal=${settings.minimalResultMode} waitOk=${settings.waitOkAfterEachCommand} commands=${sequence.joinToString { it.label }}")

        val sendResult = runCatching {
            sequence.forEach { cmd ->
                val r = session.sendDataAndWaitOk(cmd.data, cmd.label)
                if (r.isFailure) {
                    val ex = r.exceptionOrNull()
                    val message = ex?.message.orEmpty()
                    val transportFailure = isTransportFailure(message)
                    if (cmd.label == "SETTAGS" && !transportFailure && isCashRegisterRejection(message)) {
                        Log.w(TAG, "ARCUS2 SETTAGS payload rejected; retry empty SETTAGS: $message")
                        val fallback = session.sendCommandAndWaitOk("SETTAGS:")
                        if (fallback.isSuccess) {
                            Log.i(TAG, "ARCUS2 empty SETTAGS fallback accepted")
                            return@forEach
                        }
                        throw fallback.exceptionOrNull()
                            ?: IllegalStateException("ARCUS2 empty SETTAGS fallback failed")
                    }
                    if (cmd.critical || transportFailure) {
                        throw ex ?: IllegalStateException("ARCUS2 critical command failed: ${cmd.label}")
                    } else {
                        Log.w(TAG, "ARCUS2 non-critical command failed label=${cmd.label}: $message")
                    }
                }
            }
        }.map { Unit }

        lifecycleMutex.withLock {
            if (sendResult.isSuccess) {
                lifecycleState = PcEcrLifecycleState.Listening
                status.value = PcUsbConnectionStatus.WaitingForData
                Log.i(TAG, "ARCUS2 final result sent commandId=${sourceCommand.commandId ?: "-"}")
            } else {
                lifecycleState = PcEcrLifecycleState.Error
                status.value = PcUsbConnectionStatus.Error(sendResult.exceptionOrNull()?.message ?: "arcus2 send error")
                Log.e(TAG, "ARCUS2 final result failed commandId=${sourceCommand.commandId ?: "-"} error=${sendResult.exceptionOrNull()?.message}")
            }

            if (activeArcus2Transaction) {
                Log.i(TAG, "ARCUS2 active transaction cleared commandId=${activeArcus2CommandId ?: "-"}")
            }
            activeArcus2Transaction = false
            activeArcus2CommandId = null
        }
        return sendResult
    }

    override suspend fun sendArcus2StatusIfActive(
        statusText: String,
        settings: Arcus2NewWaySettings
    ): Result<Unit> {
        if (!settings.paymentStatusKeepAliveEnabled) {
            return Result.success(Unit)
        }

        val commandId = lifecycleMutex.withLock {
            val isActive = activeArcus2Transaction &&
                (lifecycleState == PcEcrLifecycleState.PausedForPayment ||
                    lifecycleState == PcEcrLifecycleState.Listening)
            if (!isActive) return@withLock "__NOT_ACTIVE__"
            activeArcus2CommandId
        }

        if (commandId == "__NOT_ACTIVE__") {
            return Result.success(Unit)
        }
        val safeCommandId = commandId?.ifBlank { "-" } ?: "-"

        val session = Arcus2CashRegisterSession(client, rawLogger, settings)
        Log.i(TAG, "ARCUS2 keep-alive STATUS send text=$statusText commandId=$safeCommandId")
        val result = runCatching {
            session.sendCommandAndWaitOk("STATUS:$statusText").getOrThrow()
        }.map { Unit }
        if (result.isFailure) {
            Log.w(
                TAG,
                "ARCUS2 keep-alive STATUS failed text=$statusText commandId=$safeCommandId error=${result.exceptionOrNull()?.message}",
                result.exceptionOrNull()
            )
        }
        return result
    }


    private suspend fun sendArcus2TransactionStartedWhileListening(
        settings: Arcus2NewWaySettings,
        statusText: String = settings.paymentStartStatusText
    ): Result<Unit> {
        val session = Arcus2CashRegisterSession(client, rawLogger, settings)
        return runCatching {
            if (settings.sendBeginTrOnPaymentStart) {
                session.sendCommandAndWaitOk("BEGINTR:").getOrThrow()
            }
            if (settings.sendStatusOnPaymentStart) {
                session.sendCommandAndWaitOk("STATUS:$statusText").getOrThrow()
            }
        }.map { Unit }
    }

    private suspend fun sendArcus2SimpleSuccessWhileListening(settings: Arcus2NewWaySettings): Result<Unit> {
        val session = Arcus2CashRegisterSession(client, rawLogger, settings)
        return runCatching {
            session.sendCommandAndWaitOk("STORERC:00").getOrThrow()
            session.sendCommandAndWaitOk("ENDTR").getOrThrow()
        }.map { Unit }
    }

    private suspend fun sendArcus2UnsupportedWhileListening(settings: Arcus2NewWaySettings, message: String): Result<Unit> {
        val session = Arcus2CashRegisterSession(client, rawLogger, settings)
        return runCatching {
            if (settings.sendStatusMessages) session.sendCommandAndWaitOk("STATUS:Операция не поддержана").getOrThrow()
            session.sendCommandAndWaitOk("STORERC:${settings.errorRc}").getOrThrow()
            session.sendCommandAndWaitOk("ENDTR").getOrThrow()
            Log.w(TAG, message)
        }.map { Unit }
    }

    private suspend fun sendArcus2ErrorWhileListening(
        settings: Arcus2NewWaySettings,
        statusText: String
    ): Result<Unit> {
        val session = Arcus2CashRegisterSession(client, rawLogger, settings)
        return runCatching {
            session.sendCommandAndWaitOk("STATUS:$statusText").getOrThrow()
            session.sendCommandAndWaitOk("STORERC:${settings.errorRc}").getOrThrow()
            session.sendCommandAndWaitOk("ENDTR").getOrThrow()
        }.map { Unit }
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
        var parsedHandled = false
        val command = when (settings.pcEcrProtocol) {
            PcEcrProtocol.CHAIOK_JSON -> PcPaymentCommandParser.parse(bytes)
            PcEcrProtocol.ARCUS2_NEWWAY -> {
                val adapter = Arcus2NewWayProtocolAdapter({ settings.arcus2NewWaySettings }, rawLogger)
                when (val parsed = adapter.parseIncoming(bytes)) {
                    is EcrParseResult.Command -> {
                        parsedHandled = true
                        when (val cmd = parsed.command) {
                        is PcEcrCommand.Payment -> {
                            val resolved = resolveArcus2FinancialCommand(cmd, settings.arcus2NewWaySettings)
                            val amount = resolved.amount
                            val currency = resolved.currency
                            Log.i(TAG, "ARCUS2 resolved financial command type=SALE amount=$amount currency=$currency orderId=${resolved.orderId ?: "-"} rrnMasked=${maskRrn(resolved.rrn)}")
                            if (amount == null || currency == null) {
                                val r = sendArcus2ErrorWhileListening(settings.arcus2NewWaySettings, "Не найдена сумма")
                                updateArcusListeningState(r, "arcus2 sale amount missing")
                                null
                            } else {
                                Log.i(TAG, "ARCUS2 start sequence commandId=${cmd.commandId ?: "-"} commands=BEGINTR,STATUS waitOk=${settings.arcus2NewWaySettings.waitOkAfterEachCommand}")
                                val startResult = sendArcus2TransactionStartedWhileListening(settings.arcus2NewWaySettings)
                                if (startResult.isFailure) {
                                    updateArcusListeningState(startResult, "arcus2 payment start response error")
                                    null
                                } else {
                                    lifecycleMutex.withLock {
                                        activeArcus2Transaction = true
                                        activeArcus2CommandId = cmd.commandId
                                        lifecycleState = PcEcrLifecycleState.PausedForPayment
                                        status.value = PcUsbConnectionStatus.Idle
                                    }
                                    Log.i(TAG, "ARCUS2 active transaction started commandId=${cmd.commandId ?: "-"}")
                                    Log.i(TAG, "ARCUS2 payment command accepted commandId=${cmd.commandId ?: "-"}")
                                    PcPaymentCommand(amount = amount, commandId = cmd.commandId, orderId = resolved.orderId ?: cmd.orderId, currency = currency, rawPayloadPreview = "arcus2", sourceProtocol = PcEcrProtocol.ARCUS2_NEWWAY)
                                }
                            }
                        }
                        is PcEcrCommand.Ping -> {
                            val r = sendArcus2SimpleSuccessWhileListening(settings.arcus2NewWaySettings)
                            updateArcusListeningState(r, "arcus2 ping error")
                            null
                        }
                        is PcEcrCommand.Reversal -> {
                            val resolved = resolveArcus2FinancialCommand(cmd, settings.arcus2NewWaySettings)
                            Log.i(TAG, "ARCUS2 resolved financial command type=REVERSAL amount=${resolved.amount} currency=${resolved.currency} orderId=${resolved.orderId ?: "-"} rrnMasked=${maskRrn(resolved.rrn)}")
                            if (resolved.rrn.isNullOrBlank()) {
                                Log.w(TAG, "ARCUS2 required data missing type=REVERSAL missing=rrn amount=${resolved.amount} currency=${resolved.currency}")
                                val r = sendArcus2ErrorWhileListening(settings.arcus2NewWaySettings, "Не найден RRN")
                                updateArcusListeningState(r, "arcus2 reversal rrn missing")
                                null
                            } else {
                                val startResult = sendArcus2TransactionStartedWhileListening(settings.arcus2NewWaySettings, statusText = "Отмена")
                                if (startResult.isFailure) {
                                    updateArcusListeningState(startResult, "arcus2 reversal start response error")
                                    null
                                } else {
                                    lifecycleMutex.withLock {
                                        activeArcus2Transaction = true
                                        activeArcus2CommandId = cmd.commandId
                                        lifecycleState = PcEcrLifecycleState.PausedForPayment
                                        status.value = PcUsbConnectionStatus.Idle
                                    }
                                    PcPaymentCommand(amount = resolved.amount ?: BigDecimal.ZERO, commandId = cmd.commandId, orderId = resolved.orderId ?: cmd.orderId, currency = resolved.currency ?: "RUB", rawPayloadPreview = "arcus2 reversal rrn=${resolved.rrn?.takeLast(4) ?: "missing"}", sourceProtocol = PcEcrProtocol.ARCUS2_NEWWAY, operationType = PcEcrOperationType.CANCEL_PREVIOUS, rrn = resolved.rrn)
                                }
                            }
                        }
                        is PcEcrCommand.Refund, is PcEcrCommand.Settlement -> {
                            // TODO: enable refundAdditionalDataEnabled only when REFUND business flow is implemented.
                            Log.i(TAG, "ARCUS2 operation parsed but unsupported type=${cmd.javaClass.simpleName.uppercase()}")
                            // TODO: apply additional data resolver when REFUND/SETTLEMENT business flow is implemented.
                            val r = sendArcus2UnsupportedWhileListening(settings.arcus2NewWaySettings, "Unsupported ARCUS2 operation")
                            updateArcusListeningState(r, "arcus2 unsupported error")
                            null
                        }
                        else -> null
                    }
                    }
                    is EcrParseResult.Ack -> {
                        parsedHandled = true
                        Log.i(TAG, "ARCUS2 standalone control response ignored")
                        null
                    }
                    is EcrParseResult.Error -> {
                        parsedHandled = true
                        val r = if (parsed.reason.contains("RRN missing", ignoreCase = true)) {
                            sendArcus2ErrorWhileListening(
                                settings.arcus2NewWaySettings,
                                "Не найден RRN"
                            )
                        } else {
                            sendArcus2UnsupportedWhileListening(
                                settings.arcus2NewWaySettings,
                                "ARCUS2 parse/protocol error: ${parsed.reason}"
                            )
                        }
                        updateArcusListeningState(r, "arcus2 parse error")
                        null
                    }
                    is EcrParseResult.Unknown -> {
                        parsedHandled = true
                        val r = sendArcus2UnsupportedWhileListening(
                            settings.arcus2NewWaySettings,
                            "ARCUS2 unknown command: ${parsed.reason}"
                        )
                        updateArcusListeningState(r, "arcus2 unknown error")
                        null
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
        } else if (!parsedHandled) {
            Log.w(TAG, "payload received but parser returned null. hex=${bytes.toHexPreview()}")
        } else {
            Log.i(TAG, "ECR payload handled without opening payment flow")
        }
    }

    private suspend fun resolveArcus2FinancialCommand(
        cmd: PcEcrCommand,
        settings: Arcus2NewWaySettings
    ): Arcus2AdditionalData {
        val shouldRequest = when (cmd) {
            is PcEcrCommand.Payment -> settings.saleAdditionalDataEnabled
            is PcEcrCommand.Reversal -> cmd.rrn.isNullOrBlank() && settings.reversalAdditionalDataEnabled
            else -> false
        }
        val additional = if (shouldRequest) requestArcus2AdditionalData(settings, cmd::class.simpleName ?: "unknown") else Arcus2AdditionalData()
        return when (cmd) {
            is PcEcrCommand.Payment -> Arcus2AdditionalData(
                amount = cmd.amount,
                currency = cmd.currency,
                orderId = cmd.orderId ?: additional.orderId,
                rrn = additional.rrn,
                rawTags = additional.rawTags
            )
            is PcEcrCommand.Reversal -> Arcus2AdditionalData(
                rrn = cmd.rrn ?: additional.rrn,
                amount = cmd.amount ?: additional.amount,
                currency = cmd.currency ?: additional.currency,
                orderId = cmd.orderId ?: additional.orderId,
                rawTags = additional.rawTags
            )
            else -> Arcus2AdditionalData()
        }
    }

    private suspend fun requestArcus2AdditionalData(settings: Arcus2NewWaySettings, reason: String): Arcus2AdditionalData {
        if (!settings.additionalDataRequestEnabled) return Arcus2AdditionalData()

        val logTag = TAG
        val requestCommand = settings.additionalDataRequestCommand
        val readTimeoutMs = settings.additionalDataReadTimeoutMs
        val maxFrames = settings.additionalDataMaxFrames
        val rrnTagKeysCsv = settings.rrnTagKeysCsv
        val rrnOwTagIdsCsv = settings.rrnOwTagIdsCsv
        val amountOwTagIdsCsv = settings.amountOwTagIdsCsv
        val currencyOwTagIdsCsv = settings.currencyOwTagIdsCsv
        val terminalIdOwTagIdsCsv = settings.terminalIdOwTagIdsCsv
        val responseCodeOwTagIdsCsv = settings.responseCodeOwTagIdsCsv

        Log.i(logTag, "ARCUS2 additional data request start reason=$reason command=$requestCommand timeoutMs=$readTimeoutMs maxFrames=$maxFrames")
        val session = Arcus2CashRegisterSession(client, rawLogger, settings)

        val result = session.sendCommandAndReadFrames(
            dataText = requestCommand,
            readTimeoutMs = readTimeoutMs,
            maxFrames = maxFrames,
            shouldStop = { responses ->
                responsesContainRequiredRrn(
                    responses = responses,
                    rrnTagKeysCsv = rrnTagKeysCsv,
                    rrnOwTagIdsCsv = rrnOwTagIdsCsv
                )
            }
        )

        if (result.isFailure) {
            val err = result.exceptionOrNull()
            Log.w(logTag, "ARCUS2 additional data request failed reason=$reason error=${err?.message}", err)
            return Arcus2AdditionalData()
        }

        val responses = result.getOrNull().orEmpty()
        if (responses.isEmpty()) {
            Log.w(logTag, "ARCUS2 additional data request returned no frames. Check additionalDataRequestCommand=$requestCommand")
        }

        val maskedResponses = responses.joinToString("|") { frame -> maskArcusText(frame.text) }.take(512)
        Log.i(logTag, "ARCUS2 additional data raw responses count=${responses.size} items=$maskedResponses")

        val tags = linkedMapOf<String, String>()
        for (frame in responses) {
            val text = frame.text.trim()
            if (text == "OK") {
                Log.i(logTag, "ARCUS2 additional data response=OK rawBytes=${frame.data.size}")
                continue
            }

            if (text == "ER" || text == "NAK") {
                Log.w(logTag, "ARCUS2 additional data returned $text rawBytes=${frame.data.size}")
                continue
            }

            val textTags = parseArcus2Tags(frame.text)
            val binaryTags = parseArcus2OwTags(
                data = frame.data,
                rrnOwTagIdsCsv = rrnOwTagIdsCsv,
                amountOwTagIdsCsv = amountOwTagIdsCsv,
                currencyOwTagIdsCsv = currencyOwTagIdsCsv,
                terminalIdOwTagIdsCsv = terminalIdOwTagIdsCsv,
                responseCodeOwTagIdsCsv = responseCodeOwTagIdsCsv
            )

            Log.i(
                logTag,
                "ARCUS2 additional data response=${maskArcusText(frame.text)} " +
                    "rawBytes=${frame.data.size} " +
                    "textTags=${maskArcusTags(textTags)} " +
                    "owTags=${maskArcusTags(binaryTags)}"
            )

            tags.putAll(textTags)
            tags.putAll(binaryTags)
        }

        val data = buildArcus2AdditionalData(tags, settings)
        val parsedLog = "ARCUS2 additional data parsed reason=$reason " +
            "rrnMasked=${maskRrn(data.rrn)} " +
            "amount=${data.amount} currency=${data.currency} " +
            "orderId=${data.orderId ?: "-"} " +
            "tags=${maskArcusTags(tags)}"
        Log.i(logTag, parsedLog)
        return data
    }

    private fun responsesContainRequiredRrn(
        responses: List<Arcus2CashRegisterSession.Arcus2ReceivedFrame>,
        rrnTagKeysCsv: String,
        rrnOwTagIdsCsv: String = "1F8431,1F03"
    ): Boolean {
        for (frame in responses) {
            val textTags = parseArcus2Tags(frame.text)
            val binaryTags = parseArcus2OwTags(
                data = frame.data,
                rrnOwTagIdsCsv = rrnOwTagIdsCsv,
                amountOwTagIdsCsv = "9F02",
                currencyOwTagIdsCsv = "5F2A",
                terminalIdOwTagIdsCsv = "9F1C",
                responseCodeOwTagIdsCsv = "1F0D"
            )
            val merged = linkedMapOf<String, String>()
            merged.putAll(textTags)
            merged.putAll(binaryTags)
            val rrn = normalizeRrn(findFirstTag(merged, rrnTagKeysCsv)) ?: normalizeRrn(merged["rrn"])
            if (rrn != null) return true
        }
        return false
    }

    private fun parseArcus2OwTags(
        data: ByteArray,
        rrnOwTagIdsCsv: String,
        amountOwTagIdsCsv: String,
        currencyOwTagIdsCsv: String,
        terminalIdOwTagIdsCsv: String,
        responseCodeOwTagIdsCsv: String
    ): Map<String, String> {
        val out = linkedMapOf<String, String>()
        val rrnTagIds = csvHexSet(rrnOwTagIdsCsv)
        val amountTagIds = csvHexSet(amountOwTagIdsCsv)
        val currencyTagIds = csvHexSet(currencyOwTagIdsCsv)
        val terminalTagIds = csvHexSet(terminalIdOwTagIdsCsv)
        val responseCodeTagIds = csvHexSet(responseCodeOwTagIdsCsv)
        var index = 0

        while (index < data.size) {
            while (index < data.size && data[index] == 0.toByte()) index++
            if (index >= data.size) break

            val tagStart = index
            val first = data[index].toInt() and 0xFF
            index++
            if ((first and 0x1F) == 0x1F) {
                while (index < data.size) {
                    val b = data[index].toInt() and 0xFF
                    index++
                    if ((b and 0x80) == 0) break
                }
            }
            if (index >= data.size) {
                Log.w(TAG, "ARCUS2 OWTags parse stopped: malformed tag at index=$index size=${data.size}")
                break
            }

            val tag = data.copyOfRange(tagStart, index).toHexString()
            val lengthByte = data[index].toInt() and 0xFF
            index++

            val length = if ((lengthByte and 0x80) == 0) {
                lengthByte
            } else {
                val count = lengthByte and 0x7F
                if (count == 0 || index + count > data.size) {
                    Log.w(TAG, "ARCUS2 OWTags parse stopped: malformed long length tag=$tag index=$index count=$count size=${data.size}")
                    break
                }
                var acc = 0
                repeat(count) {
                    acc = (acc shl 8) or (data[index].toInt() and 0xFF)
                    index++
                }
                acc
            }

            if (length < 0 || index + length > data.size) {
                Log.w(TAG, "ARCUS2 OWTags parse stopped: length exceeds buffer tag=$tag index=$index length=$length size=${data.size}")
                break
            }
            val value = data.copyOfRange(index, index + length)
            index += length
            val normalizedTag = tag.uppercase()

            when {
                normalizedTag in amountTagIds -> out["amount"] = parseBcdDigits(value).trimStart('0').ifBlank { "0" }
                normalizedTag in currencyTagIds -> out["currency"] = parseBcdDigits(value).trimStart('0').ifBlank { "0" }
                normalizedTag in terminalTagIds -> out["terminalId"] = value.toAsciiPrintable()
                normalizedTag in responseCodeTagIds -> out["responseCode"] = value.toAsciiPrintable()
                normalizedTag in rrnTagIds -> {
                    val candidate = value.toAsciiPrintable().filter { it.isDigit() }
                    if (candidate.matches(Regex("\\d{6,12}"))) {
                        out.putIfAbsent("rrn", candidate)
                        out[normalizedTag.lowercase()] = candidate
                    }
                }
                else -> {
                    val printable = value.toAsciiPrintable()
                    if (printable.isNotBlank()) out[normalizedTag.lowercase()] = printable
                }
            }
        }

        return out
    }

    private fun parseArcus2Tags(text: String): Map<String, String> {
        val payload = text.substringAfter(':', text)
        val parts = payload.split(';', '&', '\u001B', '\n', '\r').map { it.trim() }.filter { it.isNotBlank() }
        val out = linkedMapOf<String, String>()
        parts.forEach { p ->
            if (p.contains("=")) out[p.substringBefore('=').trim().lowercase()] = p.substringAfter('=').trim()
            if (p.startsWith("/r", ignoreCase = true)) out["rrn"] = p.drop(2).trim('[', ']').filter(Char::isDigit)
        }
        return out
    }

    private fun buildArcus2AdditionalData(tags: Map<String, String>, settings: Arcus2NewWaySettings): Arcus2AdditionalData {
        val rrnRaw = findFirstTag(tags, settings.rrnTagKeysCsv)
        val amountRaw = findFirstTag(tags, settings.amountTagKeysCsv)
        val currencyRaw = findFirstTag(tags, settings.currencyTagKeysCsv)
        val currency = normalizeArcusCurrency(currencyRaw)
        return Arcus2AdditionalData(
            rrn = normalizeRrn(rrnRaw),
            amount = parseArcus2AmountFlexible(amountRaw, currency),
            currency = currency,
            orderId = findFirstTag(tags, settings.orderIdTagKeysCsv),
            rawTags = tags
        )
    }

    private fun findFirstTag(tags: Map<String, String>, csvKeys: String): String? =
        csvKeys.split(",").map { it.trim().lowercase() }.firstNotNullOfOrNull { k -> tags[k]?.takeIf { it.isNotBlank() } }
    private fun normalizeRrn(raw: String?): String? {
        val d = raw?.trim()?.removePrefix("/r")?.removePrefix("/R")?.trim('[', ']')?.filter(Char::isDigit) ?: return null
        return d.takeIf { it.matches(Regex("\\d{6,12}")) }
    }
    private fun normalizeArcusCurrency(raw: String?): String? {
        val trimmed = raw?.trim()?.uppercase() ?: return null
        val numeric = trimmed.filter { it.isDigit() }.trimStart('0').ifBlank { "0" }
        return when {
            trimmed == "RUB" || numeric == "643" -> "RUB"
            trimmed == "AMD" || numeric == "51" -> "AMD"
            else -> raw.trim()
        }
    }
    private fun parseArcus2AmountFlexible(raw: String?, currency: String?): BigDecimal? {
        val t = raw?.trim()?.replace(',', '.') ?: return null
        val n = t.toBigDecimalOrNull() ?: return null
        return if (t.contains('.')) n else when (currency) {
            "RUB" -> n.movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
            "AMD" -> n.setScale(0, RoundingMode.HALF_UP)
            else -> n
        }
    }

    private fun maskRrn(rrn: String?): String = rrn?.takeLast(4)?.padStart(rrn.length, '*') ?: "<missing>"
    private fun maskArcusText(text: String): String = text
        .replace(Regex("(?i)(rrn=)\\d{6,12}")) { it.groupValues[1] + maskRrn(it.value.substringAfter("=")) }
        .replace(Regex("(?i)(r=)\\d{6,12}")) { it.groupValues[1] + maskRrn(it.value.substringAfter("=")) }
        .replace(Regex("(?i)/r\\[?\\d{6,12}\\]?")) {
            val rrn = it.value.removePrefix("/r").removePrefix("/R").trim('[', ']')
            "/r${maskRrn(rrn)}"
        }
        .replace(Regex("\\d{13,19}")) { it.value.takeLast(4).padStart(it.value.length, '*') }
        .replace("\u001B", "<ESC>")
        .take(512)
    private fun maskArcusTags(tags: Map<String, String>): String =
        tags.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            val mv = if (
                k.equals("rrn", true) ||
                k.equals("r", true) ||
                k.equals("1f8431", true) ||
                k.equals("1f03", true)
            ) maskRrn(normalizeRrn(v)) else v.take(64)
            "$k=$mv"
        }

    private fun csvHexSet(csv: String): Set<String> =
        csv.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()

    private fun parseBcdDigits(bytes: ByteArray): String = buildString {
        for (byte in bytes) {
            val v = byte.toInt() and 0xFF
            append((v ushr 4) and 0x0F)
            append(v and 0x0F)
        }
    }

    private fun ByteArray.toAsciiPrintable(): String =
        map { it.toInt() and 0xFF }
            .filter { it in 0x20..0x7E }
            .map { it.toChar() }
            .joinToString("")
            .trim('\u0000', ' ', '\n', '\r', '\t')

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private suspend fun updateArcusListeningState(
        result: Result<Unit>,
        fallbackErrorMessage: String
    ) {
        lifecycleMutex.withLock {
            if (result.isSuccess) {
                lifecycleState = PcEcrLifecycleState.Listening
                status.value = PcUsbConnectionStatus.WaitingForData
            } else {
                val message = result.exceptionOrNull()?.message ?: fallbackErrorMessage

                lifecycleState = PcEcrLifecycleState.Error
                status.value = PcUsbConnectionStatus.Error(message)

                Log.e(
                    TAG,
                    "ARCUS2 immediate response failed: $message",
                    result.exceptionOrNull()
                )
            }
        }
    }

    override suspend fun pauseForPayment(): Result<Unit> {
        val currentSettings = settingsRepository.observeSettings().first()
        val protocol = currentSettings.pcEcrProtocol

        return lifecycleMutex.withLock {
            Log.i(TAG, "ECR pause for SSP payment")

            if (lifecycleState == PcEcrLifecycleState.PausedForPayment) {
                return@withLock Result.success(Unit)
            }

            lifecycleState = PcEcrLifecycleState.PausedForPayment
            status.value = PcUsbConnectionStatus.Idle

            if (protocol == PcEcrProtocol.ARCUS2_NEWWAY) {
                Log.i(TAG, "ARCUS2 mode: keep USB transport open during SSP payment")
                return@withLock Result.success(Unit)
            }

            val result = client.pauseTransportForPayment()
            if (result.isSuccess) {
                Log.i(TAG, "ECR paused for SSP payment")
            } else {
                Log.w(TAG, "ECR pause transport failed but continue as paused", result.exceptionOrNull())
            }
            Result.success(Unit)
        }
    }

    override suspend fun resumeAfterPayment(): Result<Unit> {
        val settings = settingsRepository.observeSettings().first()
        return lifecycleMutex.withLock {
            Log.i(TAG, "ECR resume after SSP payment")

            if (lifecycleState == PcEcrLifecycleState.Stopped) {
                Log.i(TAG, "Skip ECR resume: repository stopped completely")
                return@withLock Result.success(Unit)
            }

            if (settings.pcEcrProtocol == PcEcrProtocol.ARCUS2_NEWWAY) {
                lifecycleState = PcEcrLifecycleState.Listening
                status.value = PcUsbConnectionStatus.WaitingForData
                Log.i(TAG, "ARCUS2 mode: resume listening without reopening USB transport")
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
    }

    override suspend fun stopCompletely(): Result<Unit> {
        val settings = settingsRepository.observeSettings().first()
        return lifecycleMutex.withLock {
            if (settings.pcEcrProtocol == PcEcrProtocol.ARCUS2_NEWWAY && activeArcus2Transaction) {
                lifecycleState = PcEcrLifecycleState.PausedForPayment
                status.value = PcUsbConnectionStatus.Idle
                Log.i(TAG, "ARCUS2 active transaction: ignore stopCompletely until final result is sent commandId=${activeArcus2CommandId ?: "-"}")
                return@withLock Result.success(Unit)
            }
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
    }

    private fun ByteArray.toHexPreview(limit: Int = 96): String =
        take(limit).joinToString(" ") { "%02X".format(it) }

    private fun isTransportFailure(message: String): Boolean =
        message.contains("USB service missing", ignoreCase = true) ||
            message.contains("USB device missing", ignoreCase = true) ||
            message.contains("send failed", ignoreCase = true) ||
            message.contains("transport", ignoreCase = true)

    private fun isCashRegisterRejection(message: String): Boolean =
        message.contains("Cash register returned ER", ignoreCase = true) ||
            message.contains("Cash register returned NAK", ignoreCase = true) ||
            message.contains("Unexpected ARCUS2 response", ignoreCase = true)

    private companion object {
        private const val TAG = "PcUsbEcrFlow"
        private const val LISTEN_LOOP_DELAY_MS = 300L
        private const val ERROR_VISIBLE_DELAY_MS = 2_500L
    }
}

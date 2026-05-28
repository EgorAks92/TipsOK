package com.chaiok.pos.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentCancelPreviousRequest
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.model.PosPaymentReconciliationRequest
import com.chaiok.pos.domain.repository.PosPaymentRepository
import com.skytech.smartskyposlib.ISmartSkyPos
import com.skytech.smartskyposlib.ReconciliationResult
import com.skytech.smartskyposlib.State
import com.skytech.smartskyposlib.TransactionCallback
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SmartSkyHeadlessPosPaymentRepository(
    context: Context
) : PosPaymentRepository {

    override fun reconciliation(request: PosPaymentReconciliationRequest): Flow<PosPaymentEvent> = callbackFlow {
        cancellationRequested.set(false)
        val operationId = System.currentTimeMillis().toString().takeLast(6)
        val reconciliationCallFinished = AtomicBoolean(false)
        val terminalEventDelivered = AtomicBoolean(false)
        var isBound = false
        var bindTimeoutJob: Job? = null

        Log.i(PAYMENT_TAG, "[$operationId] reconciliation called terminalId=***${request.terminalId.takeLast(4)}")
        trySend(PosPaymentEvent.Preparing)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                bindTimeoutJob?.cancel()
                Log.i(PAYMENT_TAG, "[$operationId] SSP reconciliation service connected binderNull=${serviceBinder == null}")
                val smartSkyPos = ISmartSkyPos.Stub.asInterface(serviceBinder)
                if (smartSkyPos == null) {
                    Log.e(PAYMENT_TAG, "[$operationId] SSP reconciliation smartSkyPos is null")
                    if (terminalEventDelivered.compareAndSet(false, true)) trySend(PosPaymentEvent.Error("Не удалось подключиться к платежному сервису"))
                    close()
                    return
                }
                activeService.set(smartSkyPos)
                launch(Dispatchers.IO) {
                    runCatching {
                        trySend(PosPaymentEvent.Processing)
                        Log.i(PAYMENT_TAG, "[$operationId] SSP reconciliation invoke start")
                        smartSkyPos.reconciliation()
                    }.onSuccess { result ->
                        reconciliationCallFinished.set(true)
                        Log.i(PAYMENT_TAG, "[$operationId] SSP reconciliation invoke returned ${result.toSafeReconciliationResultLog()}")
                        val event = result.toPosPaymentEvent(operationId)
                        if (terminalEventDelivered.compareAndSet(false, true)) trySend(event)
                        close()
                    }.onFailure { err ->
                        reconciliationCallFinished.set(true)
                        Log.e(PAYMENT_TAG, "[$operationId] SSP reconciliation invoke failure: ${err.message}", err)
                        val msg = when (err) {
                            is RemoteException -> "Ошибка связи с платежным сервисом"
                            else -> err.message ?: "Сверка итогов не выполнена"
                        }
                        if (terminalEventDelivered.compareAndSet(false, true)) trySend(PosPaymentEvent.Error(msg))
                        close()
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                activeService.set(null)
                Log.w(PAYMENT_TAG, "[$operationId] SSP reconciliation service disconnected finished=${reconciliationCallFinished.get()} terminalDelivered=${terminalEventDelivered.get()}")
            }

            override fun onBindingDied(name: ComponentName?) {
                activeService.set(null)
                Log.w(PAYMENT_TAG, "[$operationId] SSP reconciliation binding died")
                if (!reconciliationCallFinished.get() && terminalEventDelivered.compareAndSet(false, true)) {
                    trySend(PosPaymentEvent.Error("Соединение с платежным сервисом потеряно"))
                    close()
                }
            }

            override fun onNullBinding(name: ComponentName?) {
                activeService.set(null)
                Log.e(PAYMENT_TAG, "[$operationId] SSP reconciliation null binding")
                if (terminalEventDelivered.compareAndSet(false, true)) {
                    trySend(PosPaymentEvent.Error("Платежный сервис недоступен"))
                    close()
                }
            }
        }

        val bindIntent = Intent(SSP_SERVICE_ACTION).apply { setPackage(SSP_PACKAGE) }
        Log.i(PAYMENT_TAG, "[$operationId] SSP reconciliation bind requested package=$SSP_PACKAGE")
        isBound = runCatching { appContext.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        Log.i(PAYMENT_TAG, "[$operationId] SSP reconciliation bind result=$isBound")

        if (!isBound) {
            Log.e(PAYMENT_TAG, "[$operationId] SSP reconciliation bind failed")
            if (terminalEventDelivered.compareAndSet(false, true)) trySend(PosPaymentEvent.Error("Платежное приложение не найдено"))
            close()
            return@callbackFlow
        }

        bindTimeoutJob = launch {
            delay(SSP_BIND_TIMEOUT_MS)
            if (terminalEventDelivered.compareAndSet(false, true)) {
                Log.e(PAYMENT_TAG, "[$operationId] SSP reconciliation bind timeout")
                trySend(PosPaymentEvent.Error("Таймаут подключения к платежному сервису"))
                close()
            }
        }

        awaitClose {
            bindTimeoutJob?.cancel()
            Log.i(PAYMENT_TAG, "[$operationId] SSP reconciliation awaitClose isBound=$isBound finished=${reconciliationCallFinished.get()}")
            activeService.set(null)
            if (isBound) runCatching { appContext.unbindService(connection) }
        }
    }

    override fun cancelPreviousPayment(request: PosPaymentCancelPreviousRequest): Flow<PosPaymentEvent> = callbackFlow {
        cancellationRequested.set(false)
        val operationId = System.currentTimeMillis().toString().takeLast(6)
        val paymentCallFinished = AtomicBoolean(false)
        val terminalEventDelivered = AtomicBoolean(false)
        var isBound = false
        var bindTimeoutJob: Job? = null

        Log.i(PAYMENT_TAG, "[$operationId] cancelPreviousPayment called rrn=***${request.rrn.takeLast(4)} terminalId=***${request.terminalId.takeLast(4)} amount=${request.amount} currency=${request.currency}")
        trySend(PosPaymentEvent.Preparing)

        val callback = object : TransactionCallback.Stub() {
            override fun onStateChanged(state: Int, message: String?) {
                Log.i(
                    PAYMENT_TAG,
                    "[$operationId] SSP cancel onStateChanged code=$state name=${state.toStateLogName()} " +
                        "messagePreview=${message.toPaymentMessagePreview()}"
                )
                val event = state.toNonTerminalPaymentEvent(message)
                if (event != null && !terminalEventDelivered.get()) trySend(event)
            }
            override fun onQrReading(qrId: String?, payload: String?) = Unit
            override fun onOperationNameChanged(operationName: String?) = Unit
            override fun onRequestPassword(message: String?): String = ""
        }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                bindTimeoutJob?.cancel()
                Log.i(PAYMENT_TAG, "[$operationId] SSP cancel service connected binderNull=${serviceBinder == null}")
                val smartSkyPos = ISmartSkyPos.Stub.asInterface(serviceBinder)
                if (smartSkyPos == null) {
                    Log.e(PAYMENT_TAG, "[$operationId] SSP cancel smartSkyPos is null")
                    if (terminalEventDelivered.compareAndSet(false, true)) trySend(PosPaymentEvent.Error("Не удалось подключиться к платежному сервису"))
                    close(); return
                }
                activeService.set(smartSkyPos)
                launch(Dispatchers.IO) {
                    runCatching {
                        trySend(PosPaymentEvent.WaitingForCard)
                        Log.i(
                            PAYMENT_TAG,
                            "[$operationId] SSP cancel invoke start rrn=***${request.rrn.takeLast(4)} " +
                                "amount=${request.amount} currency=${request.currency} terminalId=***${request.terminalId.takeLast(4)}"
                        )
                        invokeCancelPreviousByRrn(smartSkyPos, request, callback)
                    }.onSuccess { result ->
                        paymentCallFinished.set(true)
                        Log.i(PAYMENT_TAG, "[$operationId] SSP cancel invoke returned rc=${result.rc} approved=${result.isApproved}")
                        val mapped = result.toPosPaymentEvent(operationId)
                        val normalized = when (mapped) {
                            is PosPaymentEvent.Approved -> mapped.copy(
                                message = mapped.message?.ifBlank { "Отмена выполнена" } ?: "Отмена выполнена"
                            )
                            is PosPaymentEvent.Declined -> mapped.copy(
                                reason = mapped.reason?.ifBlank { "Отмена не выполнена" } ?: "Отмена не выполнена"
                            )
                            is PosPaymentEvent.Error -> mapped.copy(
                                message = mapped.message.ifBlank { "Отмена не выполнена" }
                            )
                            else -> mapped
                        }
                        if (terminalEventDelivered.compareAndSet(false, true)) trySend(normalized)
                        close()
                    }.onFailure { err ->
                        paymentCallFinished.set(true)
                        Log.e(PAYMENT_TAG, "[$operationId] SSP cancel invoke failure: ${err.message}", err)
                        val msg = err.message ?: "Отмена не выполнена"
                        if (terminalEventDelivered.compareAndSet(false, true)) trySend(PosPaymentEvent.Error(msg))
                        close()
                    }
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w(PAYMENT_TAG, "[$operationId] SSP cancel service disconnected")
                activeService.set(null)
            }
        }

        val bindIntent = Intent(SSP_SERVICE_ACTION).apply { setPackage(SSP_PACKAGE) }
        Log.i(PAYMENT_TAG, "[$operationId] SSP cancel bind requested package=$SSP_PACKAGE action=$SSP_SERVICE_ACTION")
        isBound = runCatching { appContext.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        Log.i(PAYMENT_TAG, "[$operationId] SSP cancel bind result=$isBound")
        if (!isBound) {
            Log.e(PAYMENT_TAG, "[$operationId] SSP cancel bind failed")
            if (terminalEventDelivered.compareAndSet(false, true)) trySend(PosPaymentEvent.Error("Платежное приложение не найдено"))
            close(); return@callbackFlow
        }

        bindTimeoutJob = launch {
            delay(SSP_BIND_TIMEOUT_MS)
            if (terminalEventDelivered.compareAndSet(false, true)) {
                Log.e(PAYMENT_TAG, "[$operationId] SSP cancel bind timeout")
                trySend(PosPaymentEvent.Error("Таймаут подключения к платежному сервису"))
                close()
            }
        }

        awaitClose {
            bindTimeoutJob?.cancel()
            Log.i(PAYMENT_TAG, "[$operationId] SSP cancel awaitClose isBound=$isBound finished=${paymentCallFinished.get()}")
            activeService.set(null)
            if (isBound) runCatching { appContext.unbindService(connection) }
        }
    }

    // SmartSkyPosLib_v1.9.18-SR.2:
    // ISmartSkyPos.cancel(TransactionParams, TransactionCallback): TransactionResult
    // For ARCUS2 universal reversal by RRN, pass RRN via TransactionParams.setRrn(...).
    private fun invokeCancelPreviousByRrn(
        smartSkyPos: ISmartSkyPos,
        request: PosPaymentCancelPreviousRequest,
        callback: TransactionCallback
    ): TransactionResult {
        Log.i(PAYMENT_TAG, "SSP cancel smartSkyPos.cancel call start rrn=***${request.rrn.takeLast(4)}")
        val params = buildCancelPreviousParams(request)
        Log.i(
            PAYMENT_TAG,
            "SSP cancel params built terminalIdBlank=${params.terminalId?.isBlank() ?: true} " +
                "currencyCode=${params.currencyCode ?: "-"} rrnMasked=***${request.rrn.takeLast(4)} amount=${request.amount}"
        )
        return smartSkyPos.cancel(params, callback)
    }

    private fun buildCancelPreviousParams(
        request: PosPaymentCancelPreviousRequest
    ): TransactionParams {
        val params = TransactionParams(
            normalizeAmountForCurrency(request.amount, request.currency)
        )
        params.terminalId = request.terminalId
        params.currencyCode = resolveSspCurrencyCode(request.currency)
        params.rrn = request.rrn
        params.extraTransactionData = JSONObject()
            .put("rrn", request.rrn)
            .put("operationType", "CANCEL_PREVIOUS")
            .toString()
        return params
    }

    private fun normalizeAmountForCurrency(amount: BigDecimal, currency: String): BigDecimal =
        when (currency.uppercase()) {
            "AMD" -> amount.setScale(0, RoundingMode.HALF_UP)
            else -> amount.setScale(2, RoundingMode.HALF_UP)
        }

    private fun resolveSspCurrencyCode(currency: String): String =
        when (currency.uppercase()) {
            "RUB" -> CURRENCY_CODE_RUB
            "AMD" -> "051"
            else -> currency.uppercase()
        }

    private val appContext = context.applicationContext
    private val activeService = AtomicReference<ISmartSkyPos?>(null)
    private val cancellationRequested = AtomicBoolean(false)

    override fun startPayment(request: PosPaymentRequest): Flow<PosPaymentEvent> = callbackFlow {
        cancellationRequested.set(false)

        val operationId = System.currentTimeMillis().toString().takeLast(6)

        val paymentCallStarted = AtomicBoolean(false)
        val paymentCallFinished = AtomicBoolean(false)
        val terminalEventDelivered = AtomicBoolean(false)

        Log.i(
            PAYMENT_TAG,
            "[$operationId] startPayment called terminalId=***${request.terminalId.takeLast(4)}"
        )

        trySend(PosPaymentEvent.Preparing)

        var isBound = false

        val transactionCallback = object : TransactionCallback.Stub() {
            override fun onStateChanged(state: Int, message: String?) {
                Log.i(
                    PAYMENT_TAG,
                    "[$operationId] onStateChanged code=$state " +
                            "name=${state.toStateLogName()} " +
                            "messagePreview=${message.toPaymentMessagePreview()}"
                )

                val event = state.toNonTerminalPaymentEvent(message)

                Log.i(
                    PAYMENT_TAG,
                    "[$operationId] mapped state event=${event?.javaClass?.simpleName ?: "null"} " +
                            "terminalDelivered=${terminalEventDelivered.get()}"
                )

                if (event != null && !terminalEventDelivered.get()) {
                    trySend(event)
                }
            }

            override fun onQrReading(qrId: String?, payload: String?) {
                Log.i(
                    PAYMENT_TAG,
                    "[$operationId] onQrReading qrIdPresent=${!qrId.isNullOrBlank()} " +
                            "payloadPresent=${!payload.isNullOrBlank()}"
                )
            }

            override fun onOperationNameChanged(operationName: String?) {
                Log.i(
                    PAYMENT_TAG,
                    "[$operationId] onOperationNameChanged " +
                            "operationNamePreview=${operationName.toPaymentMessagePreview()}"
                )
            }

            override fun onRequestPassword(message: String?): String {
                Log.w(
                    PAYMENT_TAG,
                    "[$operationId] onRequestPassword messagePreview=${message.toPaymentMessagePreview()}"
                )
                return ""
            }
        }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                serviceBinder: IBinder?
            ) {
                Log.i(
                    PAYMENT_TAG,
                    "[$operationId] onServiceConnected binderNull=${serviceBinder == null}"
                )

                val smartSkyPos = ISmartSkyPos.Stub.asInterface(serviceBinder)

                if (smartSkyPos == null) {
                    Log.e(
                        PAYMENT_TAG,
                        "[$operationId] ISmartSkyPos interface is null"
                    )

                    if (terminalEventDelivered.compareAndSet(false, true)) {
                        trySend(
                            PosPaymentEvent.Error(
                                message = "Не удалось подключиться к платежному сервису"
                            )
                        )
                        close()
                    }

                    return
                }

                activeService.set(smartSkyPos)

                Log.i(
                    PAYMENT_TAG,
                    "[$operationId] SSP service interface created"
                )

                launch(Dispatchers.IO) {
                    paymentCallStarted.set(true)

                    runCatching {
                        trySend(PosPaymentEvent.WaitingForCard)

                        Log.i(
                            PAYMENT_TAG,
                            "[$operationId] calling smartSkyPos.payment"
                        )

                        val result = smartSkyPos.payment(
                            request.toTransactionParams(),
                            transactionCallback
                        )

                        paymentCallFinished.set(true)

                        Log.i(
                            PAYMENT_TAG,
                            "[$operationId] smartSkyPos.payment returned result=${result.toSafePaymentResultLog()}"
                        )

                        if (cancellationRequested.get()) {
                            PosPaymentEvent.Cancelled
                        } else {
                            result.toPosPaymentEvent(operationId)
                        }
                    }
                        .onSuccess { event ->
                            Log.i(
                                PAYMENT_TAG,
                                "[$operationId] payment flow success event=${event.javaClass.simpleName} " +
                                        "cancelled=${cancellationRequested.get()}"
                            )

                            if (terminalEventDelivered.compareAndSet(false, true)) {
                                trySend(event)
                            }

                            close()
                        }
                        .onFailure { error ->
                            paymentCallFinished.set(true)

                            Log.e(
                                PAYMENT_TAG,
                                "[$operationId] SSP payment call failed " +
                                        "type=${error.javaClass.name} " +
                                        "message=${error.message} " +
                                        "cancelled=${cancellationRequested.get()}",
                                error
                            )

                            val event = if (cancellationRequested.get()) {
                                PosPaymentEvent.Cancelled
                            } else {
                                val message = when (error) {
                                    is RemoteException -> "Ошибка связи с платежным сервисом"
                                    else -> error.message ?: "Не удалось провести оплату"
                                }

                                PosPaymentEvent.Error(message)
                            }

                            if (terminalEventDelivered.compareAndSet(false, true)) {
                                trySend(event)
                            }

                            close()
                        }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                activeService.set(null)

                Log.w(
                    PAYMENT_TAG,
                    "[$operationId] onServiceDisconnected " +
                            "paymentStarted=${paymentCallStarted.get()} " +
                            "paymentFinished=${paymentCallFinished.get()} " +
                            "terminalDelivered=${terminalEventDelivered.get()} " +
                            "cancelled=${cancellationRequested.get()}"
                )
            }

            override fun onBindingDied(name: ComponentName?) {
                activeService.set(null)

                Log.w(
                    PAYMENT_TAG,
                    "[$operationId] onBindingDied " +
                            "paymentStarted=${paymentCallStarted.get()} " +
                            "paymentFinished=${paymentCallFinished.get()} " +
                            "terminalDelivered=${terminalEventDelivered.get()} " +
                            "cancelled=${cancellationRequested.get()}"
                )

                if (!paymentCallStarted.get() && terminalEventDelivered.compareAndSet(false, true)) {
                    trySend(
                        PosPaymentEvent.Error(
                            message = "Соединение с платежным сервисом потеряно"
                        )
                    )
                    close()
                }
            }

            override fun onNullBinding(name: ComponentName?) {
                activeService.set(null)

                Log.e(
                    PAYMENT_TAG,
                    "[$operationId] onNullBinding"
                )

                if (terminalEventDelivered.compareAndSet(false, true)) {
                    trySend(
                        PosPaymentEvent.Error(
                            message = "Платежный сервис недоступен"
                        )
                    )
                    close()
                }
            }
        }

        val bindIntent = Intent(SSP_SERVICE_ACTION).apply {
            setPackage(SSP_PACKAGE)
        }

        isBound = runCatching {
            appContext.bindService(
                bindIntent,
                connection,
                Context.BIND_AUTO_CREATE
            )
        }.getOrDefault(false)

        Log.i(
            PAYMENT_TAG,
            "[$operationId] bindService result=$isBound"
        )

        if (!isBound) {
            Log.e(
                PAYMENT_TAG,
                "[$operationId] SSP bindService failed"
            )

            if (terminalEventDelivered.compareAndSet(false, true)) {
                trySend(
                    PosPaymentEvent.Error(
                        message = "Платежное приложение не найдено"
                    )
                )
            }

            close()
            return@callbackFlow
        }

        awaitClose {
            Log.i(
                PAYMENT_TAG,
                "[$operationId] awaitClose " +
                        "paymentStarted=${paymentCallStarted.get()} " +
                        "paymentFinished=${paymentCallFinished.get()} " +
                        "terminalDelivered=${terminalEventDelivered.get()} " +
                        "cancelled=${cancellationRequested.get()}"
            )

            runCatching {
                if (cancellationRequested.get() && !paymentCallFinished.get()) {
                    Log.i(
                        PAYMENT_TAG,
                        "[$operationId] calling cancelCardReading from awaitClose"
                    )
                    activeService.get()?.cancelCardReading()
                }
            }.onFailure { error ->
                Log.w(
                    PAYMENT_TAG,
                    "[$operationId] cancelCardReading from awaitClose failed",
                    error
                )
            }

            activeService.set(null)

            if (isBound) {
                runCatching {
                    appContext.unbindService(connection)
                    Log.i(
                        PAYMENT_TAG,
                        "[$operationId] service unbound"
                    )
                }.onFailure { error ->
                    Log.w(
                        PAYMENT_TAG,
                        "[$operationId] unbindService failed",
                        error
                    )
                }
            }
        }
    }

    override suspend fun cancelPayment() {
        cancellationRequested.set(true)

        withContext(Dispatchers.IO) {
            runCatching {
                Log.i(
                    PAYMENT_TAG,
                    "SSP user cancel method selected=cancelCardReading activeServicePresent=${activeService.get() != null}"
                )
                if (activeService.get() == null) {
                    Log.w(PAYMENT_TAG, "SSP user cancel skipped: activeService is null; possible flow closed before cancel")
                    return@runCatching
                }
                Log.i(PAYMENT_TAG, "SSP user cancel invoke start")
                activeService.get()?.cancelCardReading()
                Log.i(PAYMENT_TAG, "SSP user cancel invoke success")
            }.onFailure { error ->
                Log.w(
                    PAYMENT_TAG,
                    "SSP user cancel invoke failure",
                    error
                )
            }
        }
    }

    private fun PosPaymentRequest.toTransactionParams(): TransactionParams {
        return TransactionParams(
            amount.setScale(2, RoundingMode.HALF_UP)
        ).apply {
            currencyCode = CURRENCY_CODE_RUB
            terminalId = this@toTransactionParams.terminalId
            extraTransactionData = buildExtraTransactionData(
                waiterId = waiterId,
                tipAmount = tipAmount,
                serviceFee = serviceFee,
                feesCovered = feesCovered
            )
        }
    }

    private fun TransactionResult?.toPosPaymentEvent(operationId: String): PosPaymentEvent {
        if (this == null) {
            Log.e(
                PAYMENT_TAG,
                "[$operationId] TransactionResult is null"
            )

            return PosPaymentEvent.Error(
                message = "Платежный сервис не вернул результат"
            )
        }

        val approvalDecision = resolveSspApprovalDecision(
            isApproved = isApproved,
            rc = rc,
            code = code
        )

        Log.i(
            PAYMENT_TAG,
            "[$operationId] mapping TransactionResult " +
                    "approvedByFlag=${approvalDecision.approvedByFlag} " +
                    "approvedByRc=${approvalDecision.approvedByRc} " +
                    "declinedByRc=${approvalDecision.declinedByRc} " +
                    "code=$code " +
                    "rc=${approvalDecision.normalizedRc ?: "<blank>"} " +
                    "finalApproved=${approvalDecision.approved} " +
                    "decision=${approvalDecision.decisionReason}"
        )

        Log.i(
            PAYMENT_TAG,
            "[$operationId] TransactionResult messagePreview=${message.toPaymentMessagePreview()}"
        )

        return if (approvalDecision.approved) {
            PosPaymentEvent.Approved(
                transactionId = receiptNumber
                    .takeIf { it > 0 }
                    ?.toString(),
                rrn = rrn,
                authCode = authCode,
                message = message ?: "Оплата одобрена",
                receiptText = extractReceiptText()
            )
        } else {
            val declineMessage = message?.takeIf { it.isNotBlank() } ?: "Оплата отклонена"
            val declineCode = approvalDecision.normalizedRc ?: code
                .takeIf { it != APPROVED_CODE }
                ?.toString()

            PosPaymentEvent.Declined(
                reason = declineMessage,
                code = declineCode,
                rawMessage = message,
                receiptText = extractReceiptText()
            )
        }
    }

    private fun Int.toNonTerminalPaymentEvent(message: String?): PosPaymentEvent? {
        return when (this) {
            State.STARTUP.code -> PosPaymentEvent.Preparing

            State.CARD_READING.code,
            State.QR_AND_CARD_READING.code,
            State.USE_CHIP_READER.code,
            State.USE_MAG_READER.code,
            State.PRESENT_CARD_AGAIN.code,
            State.USE_OTHER_INTERFACE.code -> PosPaymentEvent.WaitingForCard

            State.PIN_CODE_ENTERING.code -> PosPaymentEvent.PinRequired

            State.CONNECTING.code,
            State.DATA_EXCHANGE.code -> PosPaymentEvent.Processing

            State.READY.code -> null

            State.STOPPED.code -> {
                if (cancellationRequested.get()) {
                    PosPaymentEvent.Cancelled
                } else {
                    null
                }
            }

            State.UNFINISHED_OPERATION.code -> {
                Log.w(
                    PAYMENT_TAG,
                    "SSP unfinished operation state received messagePreview=${message.toPaymentMessagePreview()}"
                )
                null
            }

            else -> {
                Log.i(
                    PAYMENT_TAG,
                    "SSP state ignored code=$this messagePreview=${message.toPaymentMessagePreview()}"
                )
                null
            }
        }
    }

    private fun buildExtraTransactionData(
        waiterId: String,
        tipAmount: Double,
        serviceFee: Double,
        feesCovered: Boolean
    ): String {
        return JSONObject()
            .put("waiterId", waiterId)
            .put("tipAmount", tipAmount.roundMoney())
            .put("serviceFee", serviceFee.roundMoney())
            .put("feesCovered", feesCovered)
            .toString()
    }

    private fun Double.roundMoney(): Double {
        return BigDecimal
            .valueOf(this)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }


    private fun ReconciliationResult?.toPosPaymentEvent(operationId: String): PosPaymentEvent {
        if (this == null) {
            Log.e(PAYMENT_TAG, "[$operationId] ReconciliationResult is null")
            return PosPaymentEvent.Error("Платежный сервис не вернул результат сверки итогов")
        }
        val receiptText = extractReceiptText()
        val reportPresent = !slip.isNullOrBlank()
        val receiptPresent = !receiptText.isNullOrBlank()
        val mappedSuccess = receiptPresent || reportPresent || isApproved == true || code == APPROVED_CODE
        Log.i(
            PAYMENT_TAG,
            "[$operationId] SSP reconciliation result mapped success=$mappedSuccess rc=${rc ?: "<blank>"} " +
                "reportPresent=$reportPresent receiptPresent=$receiptPresent"
        )
        return if (mappedSuccess) {
            PosPaymentEvent.Approved(
                transactionId = datetime?.time?.toString(),
                rrn = rrn,
                authCode = null,
                message = message ?: "Успешно",
                receiptText = receiptText
            )
        } else {
            PosPaymentEvent.Error(message ?: "Сверка итогов не выполнена", rawMessage = toString(), receiptText = receiptText)
        }
    }

    private fun ReconciliationResult.extractReceiptText(): String? {
        fun normalize(value: String?): String? = value
            ?.replace("\r\n", "\n")
            ?.replace('\r', '\n')
            ?.trim()
            ?.ifBlank { null }
        val blocks = buildList {
            normalize(slip)?.let { add(it) }
            transactionLog?.takeIf { it.isNotEmpty() }?.let { log ->
                add(
                    log.joinToString("\n") { tx ->
                        buildString {
                            append(tx.type ?: "TRANSACTION")
                            tx.amount?.let { append(" ").append(it.toPlainString()) }
                            tx.rc?.let { append(" RC=").append(it) }
                            tx.rrn?.let { append(" RRN=***").append(it.takeLast(4)) }
                        }
                    }
                )
            }
        }
        return blocks.distinct().joinToString("\n\n").ifBlank { null }
    }

    private fun ReconciliationResult?.toSafeReconciliationResultLog(): String {
        if (this == null) return "null"
        return "ReconciliationResult(" +
            "approved=$isApproved, " +
            "code=$code, " +
            "rc=${rc ?: "<blank>"}, " +
            "rrnPresent=${!rrn.isNullOrBlank()}, " +
            "slipPresent=${!slip.isNullOrBlank()}, " +
            "transactionLogSize=${transactionLog?.size ?: 0}, " +
            "messagePreview=${message.toPaymentMessagePreview()}" +
            ")"
    }

    private fun TransactionResult.extractReceiptText(): String? {
        fun normalize(value: String?): String? = value
            ?.replace("\r\n", "\n")
            ?.replace('\r', '\n')
            ?.trim()
            ?.ifBlank { null }

        val direct = listOfNotNull(
            runCatching { javaClass.getMethod("getReceipt").invoke(this) as? String }.getOrNull(),
            runCatching { javaClass.getMethod("getReceiptText").invoke(this) as? String }.getOrNull(),
            runCatching { javaClass.getMethod("getCustomerReceipt").invoke(this) as? String }.getOrNull(),
            runCatching { javaClass.getMethod("getMerchantReceipt").invoke(this) as? String }.getOrNull()
        ).mapNotNull(::normalize)

        if (direct.isNotEmpty()) {
            return direct.distinct().joinToString("\n\n")
        }

        val candidateNames = listOf(
            "receipt",
            "receiptText",
            "slip",
            "customerReceipt",
            "merchantReceipt",
            "customerSlip",
            "merchantSlip",
            "printData",
            "check",
            "receiptData"
        )

        val reflected = candidateNames.mapNotNull { name ->
            runCatching {
                val getterName = "get" + name.replaceFirstChar { char ->
                    char.uppercase()
                }

                val method = javaClass.methods.firstOrNull { candidate ->
                    candidate.parameterCount == 0 && candidate.name == getterName
                }

                normalize(method?.invoke(this) as? String)
            }.getOrNull()
        }

        if (reflected.isEmpty()) {
            Log.i(
                PAYMENT_TAG,
                "SSP receipt text not found in TransactionResult, fallback receipt will be used"
            )
            return null
        }

        return reflected.distinct().joinToString("\n\n")
    }

    private fun TransactionResult?.toSafePaymentResultLog(): String {
        if (this == null) {
            return "null"
        }

        return "TransactionResult(" +
                "isApproved=$isApproved, " +
                "rc=$rc, " +
                "code=$code, " +
                "receiptNumber=$receiptNumber, " +
                "rrnPresent=${!rrn.isNullOrBlank()}, " +
                "authCodePresent=${!authCode.isNullOrBlank()}, " +
                "messagePreview=${message.toPaymentMessagePreview()}" +
                ")"
    }

    private fun String?.toPaymentMessagePreview(): String {
        val normalized = this
            ?.replace("\n", " ")
            ?.replace("\r", " ")
            ?.trim()
            ?.take(160)

        return if (normalized.isNullOrBlank()) {
            "<blank>"
        } else {
            "\"$normalized\""
        }
    }

    private fun Int.toStateLogName(): String {
        return when (this) {
            State.STARTUP.code -> "STARTUP"
            State.CARD_READING.code -> "CARD_READING"
            State.QR_AND_CARD_READING.code -> "QR_AND_CARD_READING"
            State.USE_CHIP_READER.code -> "USE_CHIP_READER"
            State.USE_MAG_READER.code -> "USE_MAG_READER"
            State.PRESENT_CARD_AGAIN.code -> "PRESENT_CARD_AGAIN"
            State.USE_OTHER_INTERFACE.code -> "USE_OTHER_INTERFACE"
            State.PIN_CODE_ENTERING.code -> "PIN_CODE_ENTERING"
            State.CONNECTING.code -> "CONNECTING"
            State.DATA_EXCHANGE.code -> "DATA_EXCHANGE"
            State.READY.code -> "READY"
            State.STOPPED.code -> "STOPPED"
            State.UNFINISHED_OPERATION.code -> "UNFINISHED_OPERATION"
            else -> "UNKNOWN"
        }
    }

    companion object {
        private const val PAYMENT_TAG = "TipsPaymentFlow"
        private const val SSP_BIND_TIMEOUT_MS = 10_000L

        private const val SSP_PACKAGE = "com.skytech.smartskypos"
        private const val SSP_SERVICE_ACTION = "com.skytech.smartskypos.ISmartSkyPos"

        private const val CURRENCY_CODE_RUB = "643"

        private const val APPROVED_RC = "00"
        private const val APPROVED_CODE = 0

        internal fun resolveSspApprovalDecision(
            isApproved: Boolean?,
            rc: String?,
            code: Int
        ): SspApprovalDecision {
            val normalizedRc = rc
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            val approvedByFlag = isApproved == true
            val approvedByRc = normalizedRc.equals(APPROVED_RC, ignoreCase = true)
            val declinedByRc = normalizedRc != null && !approvedByRc

            val approved = when {
                declinedByRc -> false
                approvedByRc -> true
                approvedByFlag -> true
                else -> false
            }

            val decisionReason = when {
                declinedByRc -> "declined_by_non_approved_rc"
                approvedByRc -> "approved_by_rc"
                approvedByFlag -> "approved_by_isApproved_without_rc"
                else -> "declined_without_approval_signals"
            }

            return SspApprovalDecision(
                normalizedRc = normalizedRc,
                approvedByFlag = approvedByFlag,
                approvedByRc = approvedByRc,
                declinedByRc = declinedByRc,
                approved = approved,
                decisionReason = decisionReason,
                code = code
            )
        }
    }
}

internal data class SspApprovalDecision(
    val normalizedRc: String?,
    val approvedByFlag: Boolean,
    val approvedByRc: Boolean,
    val declinedByRc: Boolean,
    val approved: Boolean,
    val decisionReason: String,
    val code: Int
)

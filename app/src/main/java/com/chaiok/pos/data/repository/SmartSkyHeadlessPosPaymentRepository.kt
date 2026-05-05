package com.chaiok.pos.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.repository.PosPaymentRepository
import com.skytech.smartskyposlib.ISmartSkyPos
import com.skytech.smartskyposlib.State
import com.skytech.smartskyposlib.TransactionCallback
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SmartSkyHeadlessPosPaymentRepository(
    context: Context
) : PosPaymentRepository {

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
                    "cancelPayment called activeServicePresent=${activeService.get() != null}"
                )
                activeService.get()?.cancelCardReading()
            }.onFailure { error ->
                Log.w(
                    PAYMENT_TAG,
                    "SSP cancelPayment failed",
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
                message = message ?: "Оплата одобрена"
            )
        } else {
            val declineMessage = message?.takeIf { it.isNotBlank() } ?: "Оплата отклонена"
            val declineCode = approvalDecision.normalizedRc ?: code
                .takeIf { it != APPROVED_CODE }
                ?.toString()
            PosPaymentEvent.Declined(
                reason = declineMessage,
                code = declineCode,
                rawMessage = message
            )
        }
    }

    private fun Int.toNonTerminalPaymentEvent(message: String?): PosPaymentEvent? {
        return when (this) {
            State.STARTUP.code -> {
                PosPaymentEvent.Preparing
            }

            State.CARD_READING.code,
            State.QR_AND_CARD_READING.code,
            State.USE_CHIP_READER.code,
            State.USE_MAG_READER.code,
            State.PRESENT_CARD_AGAIN.code,
            State.USE_OTHER_INTERFACE.code -> {
                PosPaymentEvent.WaitingForCard
            }

            State.PIN_CODE_ENTERING.code -> {
                PosPaymentEvent.PinRequired
            }

            State.CONNECTING.code,
            State.DATA_EXCHANGE.code -> {
                PosPaymentEvent.Processing
            }

            State.READY.code -> {
                null
            }

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

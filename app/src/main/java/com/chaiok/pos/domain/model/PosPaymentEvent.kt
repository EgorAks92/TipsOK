package com.chaiok.pos.domain.model

sealed interface PosPaymentEvent {
    data object Preparing : PosPaymentEvent
    data object WaitingForCard : PosPaymentEvent
    data object CardDetected : PosPaymentEvent
    data object Processing : PosPaymentEvent
    data object PinRequired : PosPaymentEvent

    data class Approved(
        val transactionId: String? = null,
        val rrn: String? = null,
        val authCode: String? = null,
        val message: String? = null
    ) : PosPaymentEvent

    data class Declined(
        val reason: String? = null,
        val code: String? = null,
        val rawMessage: String? = null
    ) : PosPaymentEvent

    data class Error(
        val message: String,
        val rawMessage: String? = null
    ) : PosPaymentEvent

    data object Cancelled : PosPaymentEvent
}

fun PosPaymentEvent.toPaymentResultOrNull(): PaymentResult? {
    return when (this) {
        is PosPaymentEvent.Approved -> {
            PaymentResult.Approved(
                transactionId = transactionId,
                rrn = rrn,
                authCode = authCode,
                rawMessage = message
            )
        }

        is PosPaymentEvent.Declined -> {
            PaymentResult.Declined(
                reason = reason ?: "Оплата отклонена",
                code = code,
                rawMessage = rawMessage
            )
        }

        is PosPaymentEvent.Error -> {
            PaymentResult.Error(
                message = message
            )
        }

        PosPaymentEvent.Cancelled -> {
            PaymentResult.Declined(
                reason = "Оплата отменена",
                code = null,
                rawMessage = null
            )
        }

        PosPaymentEvent.Preparing,
        PosPaymentEvent.WaitingForCard,
        PosPaymentEvent.CardDetected,
        PosPaymentEvent.Processing,
        PosPaymentEvent.PinRequired -> null
    }
}
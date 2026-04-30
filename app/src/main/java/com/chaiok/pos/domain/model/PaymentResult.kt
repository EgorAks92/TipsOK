package com.chaiok.pos.domain.model

sealed class PaymentResult {
    data class Approved(
        val transactionId: String? = null,
        val rrn: String? = null,
        val authCode: String? = null,
        val rawMessage: String? = null
    ) : PaymentResult()

    data class Declined(
        val reason: String? = null,
        val code: String? = null,
        val rawMessage: String? = null
    ) : PaymentResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : PaymentResult()
}

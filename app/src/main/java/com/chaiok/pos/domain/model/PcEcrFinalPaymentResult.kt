package com.chaiok.pos.domain.model

sealed interface PcEcrFinalPaymentResult {
    data class Approved(
        val resultCode: String? = "00",
        val message: String? = null,
        val externalTransactionId: String? = null,
        val rrn: String? = null,
        val authCode: String? = null,
        val receiptText: String? = null
    ) : PcEcrFinalPaymentResult

    data class Declined(
        val resultCode: String? = null,
        val message: String? = null,
        val receiptText: String? = null
    ) : PcEcrFinalPaymentResult

    data class Cancelled(
        val message: String = "Cancelled by user",
        val receiptText: String? = null
    ) : PcEcrFinalPaymentResult

    data class Error(
        val message: String,
        val resultCode: String? = null,
        val receiptText: String? = null
    ) : PcEcrFinalPaymentResult
}

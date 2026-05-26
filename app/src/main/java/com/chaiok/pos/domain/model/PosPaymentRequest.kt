package com.chaiok.pos.domain.model

import java.io.Serializable
import java.math.BigDecimal

data class PosPaymentRequest(
    val amount: BigDecimal,
    val waiterId: String,
    val terminalId: String,
    val tipAmount: Double,
    val serviceFee: Double,
    val feesCovered: Boolean
) : Serializable

data class PosPaymentCancelPreviousRequest(
    val rrn: String,
    val amount: BigDecimal,
    val currency: String,
    val terminalId: String
)

sealed interface PosPaymentCancelPreviousResult {
    data class Success(
        val message: String?,
        val receiptText: String?,
        val rrn: String?
    ) : PosPaymentCancelPreviousResult

    data class Declined(
        val resultCode: String?,
        val message: String?,
        val receiptText: String?
    ) : PosPaymentCancelPreviousResult

    data class Error(
        val message: String?,
        val receiptText: String? = null
    ) : PosPaymentCancelPreviousResult
}

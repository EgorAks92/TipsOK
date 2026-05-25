package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import com.chaiok.pos.domain.model.ChaiOkEcrReceiptFrame
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class PcEcrPaymentResultMapper(
    private val receiptBuilder: PcEcrReceiptTextBuilder = PcEcrReceiptTextBuilder()
) {
    fun map(
        result: PcEcrFinalPaymentResult,
        commandId: String,
        orderId: String?,
        currency: String,
        billAmount: BigDecimal,
        tipAmount: BigDecimal,
        totalAmount: BigDecimal,
        terminalId: String?
    ): ChaiOkEcrPaymentResultFrame {
        val base = ChaiOkEcrPaymentResultFrame(
            commandId = commandId,
            orderId = orderId,
            currency = currency,
            billAmount = money(billAmount, currency),
            tipAmount = money(tipAmount, currency),
            totalAmount = money(totalAmount, currency),
            terminalId = terminalId,
            status = "error",
            success = false,
            createdAt = Instant.now().toString()
        )
        val frame = when (result) {
            is PcEcrFinalPaymentResult.Approved -> base.copy(status = "approved", success = true, resultCode = result.resultCode ?: "00", message = result.message, externalTransactionId = result.externalTransactionId, rrn = result.rrn, authCode = result.authCode)
            is PcEcrFinalPaymentResult.Declined -> base.copy(status = "declined", success = false, resultCode = result.resultCode, message = result.message)
            is PcEcrFinalPaymentResult.Cancelled -> base.copy(status = "cancelled", success = false, message = result.message)
            is PcEcrFinalPaymentResult.Error -> base.copy(status = "error", success = false, resultCode = result.resultCode, message = PcEcrSafeLogSanitizer.sanitize(result.message))
        }
        val rawReceipt = when (result) {
            is PcEcrFinalPaymentResult.Approved -> result.receiptText
            is PcEcrFinalPaymentResult.Declined -> result.receiptText
            is PcEcrFinalPaymentResult.Cancelled -> result.receiptText
            is PcEcrFinalPaymentResult.Error -> result.receiptText
        }
        val receiptText = if (rawReceipt.isNullOrBlank()) receiptBuilder.buildFallbackReceipt(frame) else receiptBuilder.buildFromSspReceipt(rawReceipt)
        return frame.copy(receipt = ChaiOkEcrReceiptFrame(text = receiptText))
    }

    private fun money(value: BigDecimal, currency: String): String {
        val scale = if (currency.equals("AMD", true)) 0 else 2
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }
}

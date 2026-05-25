package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.PaymentResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class PcEcrPaymentResultMapper(
    private val receiptBuilder: PcEcrReceiptTextBuilder = PcEcrReceiptTextBuilder()
) {
    fun map(
        paymentResult: PaymentResult,
        commandId: String,
        orderId: String?,
        currency: String,
        billAmount: BigDecimal,
        tipAmount: BigDecimal,
        terminalId: String?
    ): ChaiOkEcrPaymentResultFrame {
        val total = billAmount + tipAmount
        val base = ChaiOkEcrPaymentResultFrame(
            commandId = commandId,
            orderId = orderId,
            status = "error",
            success = false,
            currency = currency,
            billAmount = money(billAmount),
            tipAmount = money(tipAmount),
            totalAmount = money(total),
            terminalId = terminalId,
            createdAt = Instant.now().toString()
        )
        val frame = when (paymentResult) {
            is PaymentResult.Approved -> base.copy(
                status = "approved",
                success = true,
                resultCode = "00",
                message = paymentResult.rawMessage ?: "Approved",
                externalTransactionId = paymentResult.transactionId,
                rrn = paymentResult.rrn,
                authCode = paymentResult.authCode
            )
            is PaymentResult.Declined -> base.copy(
                status = "declined",
                success = false,
                resultCode = paymentResult.code,
                message = paymentResult.reason ?: paymentResult.rawMessage ?: "Declined"
            )
            is PaymentResult.Error -> base.copy(status = "error", success = false, message = paymentResult.message)
        }
        val receiptText = receiptBuilder.buildFallbackReceipt(frame)
        return frame.copy(receipt = ChaiOkEcrReceiptFrame(text = receiptText))
    }

    private fun money(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).toPlainString()
}


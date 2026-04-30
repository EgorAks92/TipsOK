package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.model.PaymentResult
import com.chaiok.pos.domain.repository.PaymentRepository

class TerminalPaymentRepository(
    private val paymentTerminalApi: PaymentTerminalApi
) : PaymentRepository {
    override suspend fun payTips(amountRub: Double): PaymentResult = paymentTerminalApi.pay(amountRub)
}

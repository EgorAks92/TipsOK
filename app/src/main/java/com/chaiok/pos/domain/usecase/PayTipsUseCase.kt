package com.chaiok.pos.domain.usecase

import com.chaiok.pos.data.repository.PaymentTerminalApi
import com.chaiok.pos.domain.model.PaymentResult

class PayTipsUseCase(
    private val paymentTerminalApi: PaymentTerminalApi
) {
    suspend operator fun invoke(amountRub: Double): PaymentResult = paymentTerminalApi.pay(amountRub)
}

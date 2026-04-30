package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.PaymentResult
import com.chaiok.pos.domain.repository.PaymentRepository

class PayTipsUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(amountRub: Double): PaymentResult = paymentRepository.payTips(amountRub)
}

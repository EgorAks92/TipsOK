package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.PosPaymentRepository

class CancelPosPaymentUseCase(
    private val posPaymentRepository: PosPaymentRepository
) {
    suspend operator fun invoke() {
        posPaymentRepository.cancelPayment()
    }
}
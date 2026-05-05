package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.repository.PosPaymentRepository
import kotlinx.coroutines.flow.Flow

class StartPosPaymentUseCase(
    private val posPaymentRepository: PosPaymentRepository
) {
    operator fun invoke(request: PosPaymentRequest): Flow<PosPaymentEvent> {
        return posPaymentRepository.startPayment(request)
    }
}
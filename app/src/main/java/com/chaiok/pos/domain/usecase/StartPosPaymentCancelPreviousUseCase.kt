package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.PosPaymentCancelPreviousRequest
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.repository.PosPaymentRepository
import kotlinx.coroutines.flow.Flow

class StartPosPaymentCancelPreviousUseCase(
    private val repository: PosPaymentRepository
) {
    operator fun invoke(request: PosPaymentCancelPreviousRequest): Flow<PosPaymentEvent> =
        repository.cancelPreviousPayment(request)
}

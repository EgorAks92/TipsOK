package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentReconciliationRequest
import com.chaiok.pos.domain.repository.PosPaymentRepository
import kotlinx.coroutines.flow.Flow

class StartPosPaymentReconciliationUseCase(
    private val repository: PosPaymentRepository
) {
    operator fun invoke(request: PosPaymentReconciliationRequest): Flow<PosPaymentEvent> =
        repository.reconciliation(request)
}

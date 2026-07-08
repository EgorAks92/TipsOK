package com.chaiok.pos.domain

import com.chaiok.pos.domain.model.PosPaymentCancelPreviousRequest
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentReconciliationRequest
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.repository.PosPaymentRepository
import com.chaiok.pos.domain.usecase.StartPosPaymentCancelPreviousUseCase
import com.chaiok.pos.domain.usecase.StartPosPaymentReconciliationUseCase
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class PosPaymentRoutingUseCaseTest {
    @Test
    fun `cancel previous use case routes to SSP cancelPrevious`() {
        val repository = RecordingPosPaymentRepository()
        val request = PosPaymentCancelPreviousRequest(
            rrn = "123456789012",
            amount = BigDecimal("10.00"),
            currency = "RUB",
            terminalId = "T1"
        )

        StartPosPaymentCancelPreviousUseCase(repository)(request)

        assertEquals(request, repository.cancelPreviousRequest)
        assertEquals(0, repository.purchaseCalls)
        assertEquals(0, repository.reconciliationCalls)
    }

    @Test
    fun `reconciliation use case routes to SSP reconciliation`() {
        val repository = RecordingPosPaymentRepository()
        val request = PosPaymentReconciliationRequest(terminalId = "T1")

        StartPosPaymentReconciliationUseCase(repository)(request)

        assertEquals(request, repository.reconciliationRequest)
        assertEquals(0, repository.purchaseCalls)
        assertEquals(0, repository.cancelPreviousCalls)
    }
}

private class RecordingPosPaymentRepository : PosPaymentRepository {
    var purchaseCalls = 0
    var cancelPreviousCalls = 0
    var reconciliationCalls = 0
    var cancelPreviousRequest: PosPaymentCancelPreviousRequest? = null
    var reconciliationRequest: PosPaymentReconciliationRequest? = null

    override fun startPayment(request: PosPaymentRequest): Flow<PosPaymentEvent> {
        purchaseCalls += 1
        return emptyFlow()
    }

    override fun cancelPreviousPayment(request: PosPaymentCancelPreviousRequest): Flow<PosPaymentEvent> {
        cancelPreviousCalls += 1
        cancelPreviousRequest = request
        return emptyFlow()
    }

    override fun reconciliation(request: PosPaymentReconciliationRequest): Flow<PosPaymentEvent> {
        reconciliationCalls += 1
        reconciliationRequest = request
        return emptyFlow()
    }

    override suspend fun cancelPayment() = Unit
}

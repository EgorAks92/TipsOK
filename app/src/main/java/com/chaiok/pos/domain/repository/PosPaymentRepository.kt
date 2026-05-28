package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.PosPaymentCancelPreviousRequest
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.model.PosPaymentReconciliationRequest
import kotlinx.coroutines.flow.Flow

interface PosPaymentRepository {
    fun startPayment(request: PosPaymentRequest): Flow<PosPaymentEvent>

    fun cancelPreviousPayment(request: PosPaymentCancelPreviousRequest): Flow<PosPaymentEvent>

    fun reconciliation(request: PosPaymentReconciliationRequest): Flow<PosPaymentEvent>

    suspend fun cancelPayment()
}
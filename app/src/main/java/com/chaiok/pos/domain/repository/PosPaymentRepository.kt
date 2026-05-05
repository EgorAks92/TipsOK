package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentRequest
import kotlinx.coroutines.flow.Flow

interface PosPaymentRepository {
    fun startPayment(request: PosPaymentRequest): Flow<PosPaymentEvent>

    suspend fun cancelPayment()
}
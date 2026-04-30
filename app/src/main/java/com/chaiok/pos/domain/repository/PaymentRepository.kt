package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.PaymentResult

interface PaymentRepository {
    suspend fun payTips(amountRub: Double): PaymentResult
}

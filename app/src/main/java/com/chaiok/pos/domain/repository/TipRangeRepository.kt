package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.TipRange

interface TipRangeRepository {
    suspend fun getTransactionRange(): Result<TipRange>
}

package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.TipRange
import kotlinx.coroutines.flow.Flow

interface TipRangeRepository {
    suspend fun refreshTransactionRange(): Result<TipRange>
    fun observeCachedTransactionRange(): Flow<TipRange?>
}

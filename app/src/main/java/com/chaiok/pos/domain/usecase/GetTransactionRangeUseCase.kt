package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.TipRange
import com.chaiok.pos.domain.repository.TipRangeRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionRangeUseCase(
    private val repository: TipRangeRepository
) {
    fun observe(): Flow<TipRange?> = repository.observeCachedTransactionRange()
    suspend fun refresh(): Result<TipRange> = repository.refreshTransactionRange()
}

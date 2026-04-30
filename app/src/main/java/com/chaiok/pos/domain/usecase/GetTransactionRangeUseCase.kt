package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.TipRange
import com.chaiok.pos.domain.repository.TipRangeRepository

class GetTransactionRangeUseCase(
    private val repository: TipRangeRepository
) {
    suspend operator fun invoke(): Result<TipRange> = repository.getTransactionRange()
}

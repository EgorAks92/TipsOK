package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.WaiterRepository

class UpdateStatusUseCase(private val waiterRepository: WaiterRepository) {
    suspend operator fun invoke(status: String) = waiterRepository.updateStatus(status)
}

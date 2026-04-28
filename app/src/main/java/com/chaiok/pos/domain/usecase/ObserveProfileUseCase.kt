package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.WaiterRepository

class ObserveProfileUseCase(private val waiterRepository: WaiterRepository) {
    operator fun invoke() = waiterRepository.observeProfile()
}

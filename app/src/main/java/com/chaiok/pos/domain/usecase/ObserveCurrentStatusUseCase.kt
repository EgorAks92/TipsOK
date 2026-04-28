package com.chaiok.pos.domain.usecase

import kotlinx.coroutines.flow.map

class ObserveCurrentStatusUseCase(
    observeProfileUseCase: ObserveProfileUseCase
) {
    private val profileFlow = observeProfileUseCase()
    operator fun invoke() = profileFlow.map { it?.status.orEmpty() }
}

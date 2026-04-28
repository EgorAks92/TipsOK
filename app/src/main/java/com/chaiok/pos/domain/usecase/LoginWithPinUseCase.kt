package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.WaiterRepository

class LoginWithPinUseCase(
    private val authRepository: AuthRepository,
    private val waiterRepository: WaiterRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(pin: String): Result<WaiterProfile> {
        val waiterId = authRepository.login(pin).getOrElse { return Result.failure(it) }
        sessionRepository.setActiveWaiter(waiterId)
        return waiterRepository.loadProfile(waiterId)
    }
}

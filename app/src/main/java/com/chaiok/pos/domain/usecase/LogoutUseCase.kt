package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.SessionRepository

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
        sessionRepository.clear()
    }
}

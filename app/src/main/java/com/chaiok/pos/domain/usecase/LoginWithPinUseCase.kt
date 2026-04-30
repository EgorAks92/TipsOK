package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.TerminalDataProvider
import com.chaiok.pos.domain.repository.WaiterRepository

class LoginWithPinUseCase(
    private val authRepository: AuthRepository,
    private val waiterRepository: WaiterRepository,
    private val sessionRepository: SessionRepository,
    private val terminalDataProvider: TerminalDataProvider
) {
    suspend operator fun invoke(pin: String): Result<WaiterProfile> {
        val terminalInfo = terminalDataProvider.getTerminalInfo().getOrElse { return Result.failure(it) }
        val waiterId = authRepository.login(pin, terminalInfo).getOrElse { return Result.failure(it) }
        sessionRepository.setActiveWaiter(waiterId)
        return waiterRepository.loadProfile(waiterId)
    }
}

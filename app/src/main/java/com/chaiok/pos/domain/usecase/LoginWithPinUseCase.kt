package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.TerminalDataProvider
import com.chaiok.pos.domain.repository.WaiterRepository
import android.util.Log

class LoginWithPinUseCase(
    private val authRepository: AuthRepository,
    private val terminalDataProvider: TerminalDataProvider,
    private val waiterRepository: WaiterRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(pin: String): Result<WaiterProfile> {
        val terminalInfo = runCatching { terminalDataProvider.getTerminalInfo() }
            .getOrElse { error ->
                val domainError = when (error) {
                    is DomainError.TerminalDataNotReady -> DomainError.TerminalDataNotReady
                    is DomainError.TerminalDataInvalid -> DomainError.TerminalDataInvalid
                    else -> DomainError.LoginFailed
                }
                return Result.failure(domainError)
            }

        val waiterId = authRepository.login(pin, terminalInfo).getOrElse { return Result.failure(it) }
        sessionRepository.setActiveWaiter(waiterId)
        return waiterRepository.loadProfile(waiterId)
    }
}

package com.chaiok.pos.domain.usecase

import android.util.Log
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.TerminalDataProvider
import com.chaiok.pos.domain.repository.WaiterRepository

class LoginWithPinUseCase(
    private val authRepository: AuthRepository,
    private val terminalDataProvider: TerminalDataProvider,
    private val waiterRepository: WaiterRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(pin: String): Result<WaiterProfile> {
        Log.e("LoginFlow", "LoginWithPinUseCase started")

        val terminalInfo = runCatching {
            terminalDataProvider.getTerminalInfo()
        }.getOrElse { error ->
            val domainError = when (error) {
                is DomainError.TerminalDataNotReady -> DomainError.TerminalDataNotReady
                is DomainError.TerminalDataInvalid -> DomainError.TerminalDataInvalid
                else -> DomainError.LoginFailed
            }

            return Result.failure(domainError)
        }

        val authSession = authRepository.login(pin, terminalInfo)
            .getOrElse { return Result.failure(it) }

        sessionRepository.setActiveWaiter(authSession.waiterId)
        sessionRepository.setProfileId(authSession.profileId)
        sessionRepository.setAccessToken(authSession.accessToken)
        sessionRepository.setTerminalId(terminalInfo.tid)

        waiterRepository
            .setCardConnected(authSession.isCardConnected)
            .getOrElse { return Result.failure(it) }

        waiterRepository
            .setServiceFeePercent(authSession.serviceFeePercent)
            .getOrElse { return Result.failure(it) }

        return waiterRepository.loadProfile(authSession.waiterId)
    }
}
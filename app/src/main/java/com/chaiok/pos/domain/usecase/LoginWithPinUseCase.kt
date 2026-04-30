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
        Log.e("LoginFlow", "LoginWithPinUseCase started")
        Log.e("LoginFlow", "getTerminalInfo started")
        val terminalInfo = runCatching { terminalDataProvider.getTerminalInfo() }
            .getOrElse { error ->
                Log.e("LoginFlow", "getTerminalInfo failed: ${error.message}", error)
                val domainError = when (error) {
                    is DomainError.TerminalDataNotReady -> DomainError.TerminalDataNotReady
                    is DomainError.TerminalDataInvalid -> DomainError.TerminalDataInvalid
                    else -> DomainError.LoginFailed
                }
                return Result.failure(domainError)
            }

        Log.e(
            "LoginFlow",
            "terminalInfo received serial=***${terminalInfo.serialNumber.takeLast(4)} tid=***${terminalInfo.tid.takeLast(4)}"
        )

        Log.e("LoginFlow", "backend login started")
        val waiterId = authRepository.login(pin, terminalInfo).getOrElse { error ->
            Log.e("LoginFlow", "backend login failed: ${error.message}", error)
            return Result.failure(error)
        }
        Log.e("LoginFlow", "backend login success waiterId=$waiterId")

        sessionRepository.setActiveWaiter(waiterId)
        val profileResult = waiterRepository.loadProfile(waiterId)
        Log.e("LoginFlow", "loadProfile result success=${profileResult.isSuccess}")
        return profileResult
    }
}

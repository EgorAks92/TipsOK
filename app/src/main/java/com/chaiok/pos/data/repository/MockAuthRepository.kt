package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.AuthSession
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {
    override suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<AuthSession> = runCatching {
        delay(600)
        if (pin == "1234") AuthSession("waiter-001", 10019L, "mock-token") else throw DomainError.InvalidPin
    }

    override suspend fun logout() = Unit
}

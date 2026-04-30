package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {
    override suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<String> = runCatching {
        delay(600)
        // TODO: Replace with real backend terminalLogin API:
        // waiterCode = pin, serialNumber = terminalInfo.serialNumber, tid = terminalInfo.tid
        if (pin == "1234") "waiter-001" else throw DomainError.InvalidPin
    }

    override suspend fun logout() = Unit
}

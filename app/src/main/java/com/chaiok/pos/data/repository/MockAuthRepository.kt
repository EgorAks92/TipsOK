package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {
    override suspend fun login(pin: String): Result<String> {
        delay(600)
        // TODO: Replace with real backend PIN verification API.
        return if (pin == "1234") Result.success("waiter-001")
        else Result.failure(IllegalArgumentException("INVALID_PIN"))
    }

    override suspend fun logout() = Unit
}

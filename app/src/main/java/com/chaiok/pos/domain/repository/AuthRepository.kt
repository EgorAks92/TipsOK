package com.chaiok.pos.domain.repository

interface AuthRepository {
    suspend fun login(pin: String): Result<String>
    suspend fun logout()
}

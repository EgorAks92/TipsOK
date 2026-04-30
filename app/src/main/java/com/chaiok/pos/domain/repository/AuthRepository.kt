package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.TerminalInfo

interface AuthRepository {
    suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<String>
    suspend fun logout()
}

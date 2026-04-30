package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.TerminalInfo

interface TerminalDataProvider {
    suspend fun getTerminalInfo(): TerminalInfo
}

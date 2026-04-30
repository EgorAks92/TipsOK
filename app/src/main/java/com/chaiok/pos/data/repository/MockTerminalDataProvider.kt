package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.TerminalDataProvider

class MockTerminalDataProvider : TerminalDataProvider {
    override suspend fun getTerminalInfo(): TerminalInfo {
        // Используется только если USE_MOCK_TERMINAL_DATA = true.
        return TerminalInfo(
            serialNumber = "mock-serial",
            tid = "mock-tid"
        )
    }
}
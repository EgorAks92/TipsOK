package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.TerminalDataProvider

class MockTerminalDataProvider : TerminalDataProvider {
    override suspend fun getTerminalInfo(): Result<TerminalInfo> {
        // TODO: Replace with SmartSkyPosTerminalApi integration.
        return Result.success(TerminalInfo(serialNumber = "mock-serial", tid = "mock-tid"))
    }
}

package com.chaiok.pos.domain

import com.chaiok.pos.data.repository.InMemorySessionRepository
import com.chaiok.pos.domain.model.AuthSession
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.TerminalDataProvider
import com.chaiok.pos.domain.repository.WaiterRepository
import com.chaiok.pos.domain.usecase.LoginWithPinUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginWithPinUseCaseTest {
    @Test
    fun `successful login returns profile`() = runTest {
        val session = InMemorySessionRepository()
        val useCase = LoginWithPinUseCase(FakeAuthRepo(), FakeTerminalDataProvider(), FakeWaiterRepo(), session)
        val result = useCase("1234")
        assertTrue(result.isSuccess)
        assertEquals("w1", result.getOrThrow().id)
        assertEquals("w1", session.activeWaiterId.first())
        assertEquals(10019L, session.profileId.first())
        assertEquals("token", session.accessToken.first())
        assertEquals("TID1", session.terminalId.first())
    }
}

private class FakeAuthRepo : AuthRepository {
    override suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<AuthSession> =
        Result.success(AuthSession("w1", 10019L, "token", false))
    override suspend fun logout() = Unit
}

private class FakeTerminalDataProvider : TerminalDataProvider {
    override suspend fun getTerminalInfo(): TerminalInfo = TerminalInfo("SN1", "TID1")
}

private class FakeWaiterRepo : WaiterRepository {
    override suspend fun setLoginProfileDisplayData(nickname: String?, personalAppeal: String?): Result<Unit> = Result.success(Unit)
    override suspend fun loadProfile(waiterId: String): Result<WaiterProfile> = Result.success(
        WaiterProfile(waiterId, "A", "B", "На смене", false, null)
    )

    override fun observeProfile(): Flow<WaiterProfile?> = MutableStateFlow(null)
    override suspend fun updateStatus(status: String): Result<Unit> = Result.success(Unit)
    override suspend fun linkCard(cardSha256: String, cardToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun setCardConnected(isConnected: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun setServiceFeePercent(percent: Double): Result<Unit> = Result.success(Unit)
}

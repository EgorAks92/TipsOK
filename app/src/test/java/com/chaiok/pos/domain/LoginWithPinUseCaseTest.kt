package com.chaiok.pos.domain

import com.chaiok.pos.domain.model.AuthSession
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.TerminalDataProvider
import com.chaiok.pos.domain.repository.TipRangeRepository
import com.chaiok.pos.domain.repository.WaiterRepository
import com.chaiok.pos.domain.usecase.LoginWithPinUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginWithPinUseCaseTest {
    @Test
    fun `successful login returns profile`() = runTest {
        val useCase = LoginWithPinUseCase(FakeAuthRepo(), FakeTerminalDataProvider(), FakeWaiterRepo(), FakeSessionRepo(), FakeTipRangeRepo())
        val result = useCase("1234")
        assertTrue(result.isSuccess)
    }
}

private class FakeTipRangeRepo : TipRangeRepository {
    override suspend fun refreshTransactionRange() = Result.failure<Nothing>(IllegalStateException("not needed"))
    override fun observeCachedTransactionRange() = MutableStateFlow(null)
}

private class FakeAuthRepo : AuthRepository {
    override suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<AuthSession> =
        Result.success(AuthSession("w1", 10019L, "token", false))
    override suspend fun logout() = Unit
}

private class FakeSessionRepo : SessionRepository {
    private val flow = MutableStateFlow<String?>(null)
    private val profile = MutableStateFlow<Long?>(null)
    private val token = MutableStateFlow<String?>(null)
    override val activeWaiterId: Flow<String?> = flow
    override val profileId: Flow<Long?> = profile
    override val accessToken: Flow<String?> = token
    override suspend fun setActiveWaiter(waiterId: String?) { flow.value = waiterId }
    override suspend fun setProfileId(profileId: Long?) { profile.value = profileId }
    override suspend fun setAccessToken(accessToken: String?) { token.value = accessToken }
    override suspend fun clear() { flow.value = null; profile.value = null; token.value = null }
}

private class FakeWaiterRepo : WaiterRepository {
    override suspend fun loadProfile(waiterId: String): Result<WaiterProfile> = Result.success(
        WaiterProfile(waiterId, "A", "B", "На смене", false, null)
    )

    override fun observeProfile(): Flow<WaiterProfile?> = MutableStateFlow(null)
    override suspend fun updateStatus(status: String): Result<Unit> = Result.success(Unit)
    override suspend fun linkCard(cardSha256: String, cardToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun setCardConnected(isConnected: Boolean): Result<Unit> = Result.success(Unit)
}

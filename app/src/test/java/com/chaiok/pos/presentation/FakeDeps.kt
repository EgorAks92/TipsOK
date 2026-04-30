package com.chaiok.pos.presentation

import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.AuthSession
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.SettingsRepository
import com.chaiok.pos.domain.repository.WaiterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeObserveWaiterRepo(private val profileFlow: MutableStateFlow<WaiterProfile?>) : WaiterRepository {
    override suspend fun loadProfile(waiterId: String): Result<WaiterProfile> = Result.failure(IllegalStateException())
    override fun observeProfile(): Flow<WaiterProfile?> = profileFlow
    override suspend fun updateStatus(status: String): Result<Unit> = Result.success(Unit)
    override suspend fun linkCard(cardSha256: String, cardToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun setCardConnected(isConnected: Boolean): Result<Unit> = Result.success(Unit)
}

class FakeSettingsRepo(private val settingsFlow: MutableStateFlow<AppSettings>) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = settingsFlow
    override suspend fun setIntegrationMode(enabled: Boolean) = Unit
    override suspend fun setTableMode(enabled: Boolean) = Unit
    override suspend fun setTileBackground(background: String) = Unit
}

class FakeAuthRepo2 : AuthRepository {
    override suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<AuthSession> =
        Result.success(AuthSession("1", 10019L, "token", true))
    override suspend fun logout() = Unit
}

class FakeSessionRepo2 : SessionRepository {
    override val activeWaiterId: Flow<String?> = MutableStateFlow(null)
    override val profileId: Flow<Long?> = MutableStateFlow(null)
    override val accessToken: Flow<String?> = MutableStateFlow(null)
    override suspend fun setActiveWaiter(waiterId: String?) = Unit
    override suspend fun setProfileId(profileId: Long?) = Unit
    override suspend fun setAccessToken(accessToken: String?) = Unit
    override suspend fun clear() = Unit
}

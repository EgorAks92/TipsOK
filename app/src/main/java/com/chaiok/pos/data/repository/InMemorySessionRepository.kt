package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemorySessionRepository : SessionRepository {
    private val active = MutableStateFlow<String?>(null)
    private val profile = MutableStateFlow<Long?>(null)
    private val token = MutableStateFlow<String?>(null)

    override val activeWaiterId: Flow<String?> = active.asStateFlow()
    override val profileId: Flow<Long?> = profile.asStateFlow()
    override val accessToken: Flow<String?> = token.asStateFlow()

    override suspend fun setActiveWaiter(waiterId: String?) {
        active.value = waiterId
    }

    override suspend fun setProfileId(profileId: Long?) {
        profile.value = profileId
    }

    override suspend fun setAccessToken(accessToken: String?) {
        token.value = accessToken
    }

    override suspend fun clear() {
        active.value = null
        profile.value = null
        token.value = null
    }
}

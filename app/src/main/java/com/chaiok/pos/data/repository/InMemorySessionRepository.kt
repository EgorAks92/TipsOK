package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemorySessionRepository : SessionRepository {
    private val active = MutableStateFlow<String?>(null)
    override val activeWaiterId: Flow<String?> = active.asStateFlow()

    override suspend fun setActiveWaiter(waiterId: String?) {
        active.value = waiterId
    }
}

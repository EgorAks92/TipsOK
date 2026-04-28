package com.chaiok.pos.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val activeWaiterId: Flow<String?>
    suspend fun setActiveWaiter(waiterId: String?)
}

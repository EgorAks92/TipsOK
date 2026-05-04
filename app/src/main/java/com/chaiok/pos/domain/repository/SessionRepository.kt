package com.chaiok.pos.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val activeWaiterId: Flow<String?>
    val profileId: Flow<Long?>
    val accessToken: Flow<String?>
    val terminalId: Flow<String?>

    suspend fun setActiveWaiter(waiterId: String?)
    suspend fun setProfileId(profileId: Long?)
    suspend fun setAccessToken(accessToken: String?)
    suspend fun setTerminalId(terminalId: String?)
    suspend fun clear()
}
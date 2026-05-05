package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.WaiterProfile
import kotlinx.coroutines.flow.Flow

interface WaiterRepository {
    suspend fun loadProfile(waiterId: String): Result<WaiterProfile>
    suspend fun setLoginProfileDisplayData(nickname: String?, personalAppeal: String?): Result<Unit>
    fun observeProfile(): Flow<WaiterProfile?>
    suspend fun updateStatus(status: String): Result<Unit>
    suspend fun linkCard(cardSha256: String, cardToken: String): Result<Unit>
    suspend fun setCardConnected(isConnected: Boolean): Result<Unit>
    suspend fun setServiceFeePercent(percent: Double): Result<Unit>
}

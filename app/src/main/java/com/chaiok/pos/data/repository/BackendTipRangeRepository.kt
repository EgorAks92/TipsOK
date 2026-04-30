package com.chaiok.pos.data.repository

import android.util.Log
import com.chaiok.pos.data.remote.TerminalApi
import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.TipRange
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.TipRangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BackendTipRangeRepository(
    private val api: TerminalApi,
    private val sessionRepository: SessionRepository,
    private val appDataStore: AppDataStore
) : TipRangeRepository {
    override suspend fun refreshTransactionRange(): Result<TipRange> = runCatching {
        val token = sessionRepository.accessToken.first()
        if (token.isNullOrBlank()) throw DomainError.LoginFailed

        Log.e("TipsFlow", "refreshTransactionRange started accessToken found=true")
        val response = api.getTransactionRange(authorization = "Bearer $token")
        Log.e("TipsFlow", "refreshTransactionRange response httpCode=${response.code()} isSuccessful=${response.isSuccessful}")
        if (!response.isSuccessful) throw DomainError.LoginFailed

        val body = response.body() ?: throw DomainError.LoginFailed
        Log.e("TipsFlow", "refreshTransactionRange status=${body.status} statusCode=${body.statusCode}")
        val ok = body.status.equals("SUCCESS", true) || body.statusCode.equals("SUCCESS", true)
        if (!ok) throw DomainError.LoginFailed

        val data = body.data ?: throw DomainError.LoginFailed
        Log.e("TipsFlow", "refreshTransactionRange percents count=${data.allTransactionRange.orEmpty().size}")
        val remoteRange = TipRange(
            percents = data.allTransactionRange.orEmpty(),
            startRange = data.startRange ?: 0,
            finishRange = data.finishRange ?: 0,
            defaultIndex = data.defaultIndex ?: 0
        )
        if (remoteRange.percents.isEmpty()) {
            return@runCatching remoteRange
        }

        val cachedRange = appDataStore.tipRangeFlow.first()
        if (cachedRange != remoteRange) {
            appDataStore.setTipRange(remoteRange)
        }
        remoteRange
    }.onFailure { Log.e("TipsFlow", "refreshTransactionRange failed: ${it.message}", it) }

    override fun observeCachedTransactionRange(): Flow<TipRange?> = appDataStore.tipRangeFlow
}

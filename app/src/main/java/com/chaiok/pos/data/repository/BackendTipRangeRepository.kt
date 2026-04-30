package com.chaiok.pos.data.repository

import android.util.Log
import com.chaiok.pos.data.remote.TerminalApi
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.TipRange
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.TipRangeRepository
import kotlinx.coroutines.flow.first

class BackendTipRangeRepository(
    private val api: TerminalApi,
    private val sessionRepository: SessionRepository
) : TipRangeRepository {
    override suspend fun getTransactionRange(): Result<TipRange> = runCatching {
        val token = sessionRepository.accessToken.first()
        if (token.isNullOrBlank()) throw DomainError.LoginFailed

        val response = api.getTransactionRange(authorization = "Bearer $token")
        if (!response.isSuccessful) throw DomainError.LoginFailed

        val body = response.body() ?: throw DomainError.LoginFailed
        val ok = body.status.equals("SUCCESS", true) || body.statusCode.equals("SUCCESS", true)
        if (!ok) throw DomainError.LoginFailed

        val data = body.data ?: throw DomainError.LoginFailed
        TipRange(
            percents = data.allTransactionRange.orEmpty(),
            startRange = data.startRange ?: 0,
            finishRange = data.finishRange ?: 0,
            defaultIndex = data.defaultIndex ?: 0
        )
    }.onFailure { Log.e("TipsFlow", "getTransactionRange failed: ${it.message}", it) }
}

package com.chaiok.pos.data.repository

import android.util.Log
import com.chaiok.pos.data.remote.TerminalApi
import com.chaiok.pos.data.remote.dto.GetTransactionsRequestDto
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.TipRecord
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.TipsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class BackendTipsRepository(
    private val api: TerminalApi,
    private val sessionRepository: SessionRepository
) : TipsRepository {
    override suspend fun getTips(): Result<List<TipRecord>> = runCatching {
        val profileId = sessionRepository.profileId.first()
        val accessToken = sessionRepository.accessToken.first()

        if (profileId == null) throw DomainError.ProfileNotFound
        if (accessToken.isNullOrBlank()) throw DomainError.LoginFailed

        Log.e("TipsFlow", "getTransactions started profileId=$profileId accessToken found=true")
        val response = api.getTransactions(
            authorization = "Bearer $accessToken",
            request = GetTransactionsRequestDto(profileId = profileId, gift = 1)
        )
        Log.e("TipsFlow", "getTransactions response httpCode=${response.code()} isSuccessful=${response.isSuccessful}")
        if (!response.isSuccessful) throw DomainError.LoginFailed

        val body = response.body() ?: throw DomainError.LoginFailed
        Log.e("TipsFlow", "getTransactions status=${body.status}")
        if (!body.status.isSuccessStatus()) throw DomainError.LoginFailed

        val tips = body.data.orEmpty().mapNotNull { dto ->
            val dt = dto.date?.let {
                runCatching { LocalDateTime.parse(it) }.getOrNull()
            } ?: return@mapNotNull null
            val amount = dto.amount ?: 0.0
            val percent = dto.billPercentage ?: 0.0
            val billAmount = if (percent > 0.0) amount * 100.0 / percent else 0.0
            // TODO: replace calculated billAmount when backend starts returning bill amount.
            TipRecord(
                id = (dto.id ?: 0L).toString(),
                dateTime = dt,
                billAmount = billAmount,
                tipPercent = percent.toInt(),
                tipAmount = amount
            )
        }
        Log.e("TipsFlow", "getTransactions items count=${tips.size}")
        tips
    }.onFailure { Log.e("TipsFlow", "getTransactions failed: ${it.message}", it) }
}

private fun String?.isSuccessStatus(): Boolean = equals("OK", true) || equals("SUCCESS", true)

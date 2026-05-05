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

        Log.i("TipsFlow", "getTransactions started profileId=$profileId accessToken found=true")
        val response = api.getTransactions(
            authorization = "Bearer $accessToken",
            request = GetTransactionsRequestDto(profileId = profileId, gift = 1)
        )
        Log.i("TipsFlow", "getTransactions response httpCode=${response.code()} isSuccessful=${response.isSuccessful}")
        if (!response.isSuccessful) throw DomainError.LoginFailed

        val body = response.body() ?: throw DomainError.LoginFailed
        Log.i("TipsFlow", "getTransactions status=${body.status}")
        if (!body.status.isSuccessStatus()) throw DomainError.LoginFailed

        val tips = body.data.orEmpty().mapNotNull { dto ->
            val dt = dto.date?.let {
                runCatching { LocalDateTime.parse(it) }.getOrNull()
            } ?: return@mapNotNull null
            val amount = dto.amount ?: 0.0
            val percent = dto.billPercentage ?: 0.0
            TipRecord(
                id = (dto.id ?: 0L).toString(),
                dateTime = dt,
                billAmount = 0.0,
                tipPercent = percent.toInt(),
                tipAmount = amount,
                kitchenEvaluation = dto.kitchenEvaluation
                    .or(dto.kitchenRating)
                    .or(dto.kitchenAssessment)
                    .or(dto.kitchenAssessmentValue)
                    .or(dto.kitchenScore)
                    .toNormalizedRating(),
                serviceEvaluation = dto.serviceEvaluation
                    .or(dto.serviceRating)
                    .or(dto.serviceAssessment)
                    .or(dto.serviceAssessmentValue)
                    .or(dto.serviceScore)
                    .toNormalizedRating()
            )
        }
        Log.i("TipsFlow", "getTransactions items count=${tips.size}")
        tips
    }.onFailure { Log.e("TipsFlow", "getTransactions failed: ${it.message}", it) }
}

private fun String?.isSuccessStatus(): Boolean = equals("OK", true) || equals("SUCCESS", true)

private fun Any?.or(other: Any?): Any? = this ?: other

private fun Any?.toNormalizedRating(): Int? {
    val raw = when (this) {
        is Number -> {
            val doubleValue = this.toDouble()
            if (doubleValue % 1.0 == 0.0) doubleValue.toInt() else null
        }
        is String -> this.trim().toIntOrNull()
        else -> null
    }
    return raw?.takeIf { it in 1..5 }
}

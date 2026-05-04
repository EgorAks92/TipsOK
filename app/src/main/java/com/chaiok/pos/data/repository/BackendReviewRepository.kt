package com.chaiok.pos.data.repository

import android.util.Log
import com.chaiok.pos.data.remote.TerminalApi
import com.chaiok.pos.data.remote.dto.AddReviewRequestDto
import com.chaiok.pos.domain.repository.ReviewRepository
import com.chaiok.pos.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

class BackendReviewRepository(
    private val api: TerminalApi,
    private val sessionRepository: SessionRepository
) : ReviewRepository {

    override suspend fun addReview(
        kitchenEvaluation: Int,
        serviceEvaluation: Int,
        comment: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedKitchenEvaluation = kitchenEvaluation.coerceIn(1, 5)
            val normalizedServiceEvaluation = serviceEvaluation.coerceIn(1, 5)

            val profileId = sessionRepository.profileId.first()
                ?: throw IllegalStateException("ProfileId is missing")

            val accessToken = sessionRepository.accessToken.first()
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Access token is missing")

            val request = AddReviewRequestDto(
                id = 0L,
                profileId = profileId,
                clientId = 0L,
                kitchenEvaluation = normalizedKitchenEvaluation,
                serviceEvaluation = normalizedServiceEvaluation,
                comment = comment
            )

            Log.i(
                REVIEW_TAG,
                "addReview request profileId=$profileId kitchen=$normalizedKitchenEvaluation service=$normalizedServiceEvaluation"
            )

            val response = api.addReview(
                authorization = accessToken.asAuthorizationHeader(),
                request = request
            )

            Log.i(
                REVIEW_TAG,
                "addReview response httpCode=${response.code()} isSuccessful=${response.isSuccessful}"
            )

            if (!response.isSuccessful) {
                throw IOException("addReview failed httpCode=${response.code()}")
            }

            val body = response.body()
                ?: throw IOException("addReview body is null")

            val status = body.status.orEmpty()
            val statusCode = body.statusCode.orEmpty()

            Log.i(
                REVIEW_TAG,
                "addReview body status=$status statusCode=$statusCode data=${body.data}"
            )

            val isOk = status.equals("OK", ignoreCase = true) ||
                    status.equals("SUCCESS", ignoreCase = true)

            val hasBusinessError = statusCode.isNotBlank() &&
                    !statusCode.equals("OK", ignoreCase = true) &&
                    !statusCode.equals("SUCCESS", ignoreCase = true)

            if (!isOk || hasBusinessError) {
                throw IOException(
                    "addReview business error status=$status statusCode=$statusCode data=${body.data}"
                )
            }

            Unit
        }.onFailure { error ->
            Log.e(REVIEW_TAG, "addReview failed", error)
        }
    }

    private fun String.asAuthorizationHeader(): String {
        return if (startsWith("Bearer ", ignoreCase = true)) {
            this
        } else {
            "Bearer $this"
        }
    }

    private companion object {
        private const val REVIEW_TAG = "TipsReviewFlow"
    }
}
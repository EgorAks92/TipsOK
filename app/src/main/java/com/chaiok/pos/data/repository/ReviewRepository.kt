package com.chaiok.pos.domain.repository

interface ReviewRepository {
    suspend fun addReview(
        kitchenEvaluation: Int,
        serviceEvaluation: Int,
        comment: String = ""
    ): Result<Unit>
}
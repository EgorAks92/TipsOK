package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.ReviewRepository

class AddReviewUseCase(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(
        kitchenEvaluation: Int,
        serviceEvaluation: Int,
        comment: String = ""
    ): Result<Unit> {
        return reviewRepository.addReview(
            kitchenEvaluation = kitchenEvaluation,
            serviceEvaluation = serviceEvaluation,
            comment = comment
        )
    }
}
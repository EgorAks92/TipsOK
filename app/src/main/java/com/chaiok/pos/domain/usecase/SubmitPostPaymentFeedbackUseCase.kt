package com.chaiok.pos.domain.usecase

import android.util.Log
import com.chaiok.pos.domain.model.PostPaymentFeedbackPayload

class SubmitPostPaymentFeedbackUseCase(
    private val addReviewUseCase: AddReviewUseCase
) {
    suspend operator fun invoke(payload: PostPaymentFeedbackPayload): Result<Unit> {
        val serviceRating = payload.serviceRating?.coerceIn(1, 5)
        val kitchenRating = payload.kitchenRating?.coerceIn(1, 5)

        if (serviceRating == null && kitchenRating == null) {
            return Result.success(Unit)
        }

        return if (serviceRating != null && kitchenRating != null) {
            addReviewUseCase(
                serviceEvaluation = serviceRating,
                kitchenEvaluation = kitchenRating,
                comment = payload.toSafeComment()
            )
        } else {
            Log.i(
                TAG,
                "POST_PAYMENT_FEEDBACK partial feedback accepted locally; backend partial-rating endpoint TODO commandId=${payload.commandId}"
            )
            Result.success(Unit)
        }
    }

    private fun PostPaymentFeedbackPayload.toSafeComment(): String = buildString {
        append("post_payment_feedback")
        append(" commandId=").append(commandId)
        transactionId?.takeIf { it.isNotBlank() }?.let { append(" transactionId=").append(it) }
        waiterId?.takeIf { it.isNotBlank() }?.let { append(" waiterId=").append(it) }
        profileId?.let { append(" profileId=").append(it) }
        orderId?.takeIf { it.isNotBlank() }?.let { append(" orderId=").append(it) }
        billAmount?.let { append(" billAmount=").append(it.toPlainString()) }
        tipAmount?.let { append(" tipAmount=").append(it.toPlainString()) }
        append(" createdAt=").append(createdAt)
    }

    private companion object {
        private const val TAG = "PcCommandIdle"
    }
}

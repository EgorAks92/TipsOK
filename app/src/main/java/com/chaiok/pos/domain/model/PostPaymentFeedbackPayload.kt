package com.chaiok.pos.domain.model

import java.math.BigDecimal
import java.time.Instant

data class PostPaymentFeedbackPayload(
    val commandId: String,
    val transactionId: String? = null,
    val waiterId: String? = null,
    val profileId: Long? = null,
    val orderId: String? = null,
    val billAmount: BigDecimal? = null,
    val tipAmount: BigDecimal? = null,
    val serviceRating: Int? = null,
    val kitchenRating: Int? = null,
    val createdAt: String = Instant.now().toString()
)

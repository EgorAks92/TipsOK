package com.chaiok.pos.domain.model

import java.math.BigDecimal

data class PcPaymentCommand(
    val amount: BigDecimal,
    val commandId: String? = null,
    val orderId: String? = null,
    val rawPayloadPreview: String? = null
)

package com.chaiok.pos.domain.model

import java.math.BigDecimal

data class PcPaymentCommand(
    val amount: BigDecimal,
    val commandId: String? = null,
    val currency: String = "RUB",
    val orderId: String? = null,
    val rawPayloadPreview: String? = null,
    val operationType: PcEcrOperationType = PcEcrOperationType.SALE,
    val rrn: String? = null,
    val waiterPin: String? = null
)

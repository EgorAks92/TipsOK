package com.chaiok.pos.domain.model

import java.io.Serializable
import java.math.BigDecimal

data class PosPaymentRequest(
    val amount: BigDecimal,
    val waiterId: String,
    val terminalId: String,
    val tipAmount: Double,
    val serviceFee: Double,
    val feesCovered: Boolean
) : Serializable
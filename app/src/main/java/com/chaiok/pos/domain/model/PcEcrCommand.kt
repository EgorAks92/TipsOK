package com.chaiok.pos.domain.model

import java.math.BigDecimal

sealed interface PcEcrCommand {
    val commandId: String?

    data class Payment(override val commandId: String?, val orderId: String?, val amount: BigDecimal, val currency: String) : PcEcrCommand
    data class Reversal(override val commandId: String?, val orderId: String?, val rrn: String?, val amount: BigDecimal?, val currency: String?) : PcEcrCommand
    data class Refund(override val commandId: String?, val amount: BigDecimal?, val currency: String?) : PcEcrCommand
    data class Settlement(override val commandId: String?) : PcEcrCommand
    data class Reconciliation(override val commandId: String?) : PcEcrCommand
    data class WaiterLogin(override val commandId: String?, val waiterPin: String?) : PcEcrCommand
    data class Ping(override val commandId: String?) : PcEcrCommand
    data class Unknown(override val commandId: String?, val reason: String, val rawHexPreview: String) : PcEcrCommand
}

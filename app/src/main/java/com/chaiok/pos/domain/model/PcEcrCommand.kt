package com.chaiok.pos.domain.model

import java.math.BigDecimal

sealed interface PcEcrCommand {
    val commandId: String?
    val rawProtocol: PcEcrProtocol

    data class Payment(override val commandId: String?, override val rawProtocol: PcEcrProtocol, val orderId: String?, val amount: BigDecimal, val currency: String) : PcEcrCommand
    data class Reversal(override val commandId: String?, override val rawProtocol: PcEcrProtocol, val orderId: String?, val rrn: String?, val amount: BigDecimal?, val currency: String?) : PcEcrCommand
    data class Refund(override val commandId: String?, override val rawProtocol: PcEcrProtocol, val amount: BigDecimal?, val currency: String?) : PcEcrCommand
    data class Settlement(override val commandId: String?, override val rawProtocol: PcEcrProtocol) : PcEcrCommand
    data class Ping(override val commandId: String?, override val rawProtocol: PcEcrProtocol) : PcEcrCommand
    data class Unknown(override val commandId: String?, override val rawProtocol: PcEcrProtocol, val reason: String, val rawHexPreview: String) : PcEcrCommand
}

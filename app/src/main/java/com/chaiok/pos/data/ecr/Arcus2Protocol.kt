package com.chaiok.pos.data.ecr

import java.math.BigDecimal
import java.nio.charset.Charset

private val WIN1251: Charset = Charset.forName("windows-1251")

data class Arcus2BinLenFrame(val data: ByteArray)

object Arcus2BinLenCodec {
    const val SOH: Byte = 0x01
    fun decode(bytes: ByteArray): Result<Arcus2BinLenFrame> = runCatching {
        require(bytes.size >= 3) { "Frame is too short" }
        require(bytes[0] == SOH) { "Wrong SOH" }
        val len = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
        require(bytes.size >= 3 + len) { "Length mismatch" }
        Arcus2BinLenFrame(bytes.copyOfRange(3, 3 + len))
    }

    fun encode(data: ByteArray): ByteArray {
        require(data.size <= 0xFFFF)
        val high = ((data.size shr 8) and 0xFF).toByte()
        val low = (data.size and 0xFF).toByte()
        return byteArrayOf(SOH, high, low) + data
    }
}

fun ByteArray.toHexPreview(limit: Int = 128): String = take(limit).joinToString(" ") { "%02X".format(it) }
fun decodeWin1251(data: ByteArray): String = data.toString(WIN1251)
fun encodeWin1251(text: String): ByteArray = text.toByteArray(WIN1251)

enum class EcrProtocol { CHAIOK_JSON, ARCUS2_NEWWAY }
sealed interface PcEcrCommand { val rawProtocol: com.chaiok.pos.domain.model.PcEcrProtocol; val commandId: String?
    data class Payment(override val commandId: String?, override val rawProtocol: com.chaiok.pos.domain.model.PcEcrProtocol, val orderId: String?, val amount: BigDecimal, val currency: String): PcEcrCommand
    data class Reversal(override val commandId: String?, override val rawProtocol: com.chaiok.pos.domain.model.PcEcrProtocol, val orderId: String?, val rrn: String?, val amount: BigDecimal?, val currency: String?): PcEcrCommand
    data class Refund(override val commandId: String?, override val rawProtocol: com.chaiok.pos.domain.model.PcEcrProtocol, val amount: BigDecimal?, val currency: String?): PcEcrCommand
    data class Settlement(override val commandId: String?, override val rawProtocol: com.chaiok.pos.domain.model.PcEcrProtocol): PcEcrCommand
    data class Ping(override val commandId: String?, override val rawProtocol: com.chaiok.pos.domain.model.PcEcrProtocol): PcEcrCommand
    data class Unknown(override val commandId: String?, override val rawProtocol: com.chaiok.pos.domain.model.PcEcrProtocol, val reason: String, val rawHexPreview: String): PcEcrCommand
}

sealed interface EcrParseResult {
    data class Command(val command: PcEcrCommand) : EcrParseResult
    data class Ack(val bytesToSend: ByteArray? = null) : EcrParseResult
    data class Unknown(val reason: String, val rawHexPreview: String) : EcrParseResult
    data class Error(val reason: String, val bytesToSend: ByteArray? = null) : EcrParseResult
}

interface EcrProtocolAdapter { val protocol: com.chaiok.pos.domain.model.PcEcrProtocol; fun parseIncoming(bytes: ByteArray): EcrParseResult }

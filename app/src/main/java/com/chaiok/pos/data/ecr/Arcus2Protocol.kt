package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.PcEcrCommand
import com.chaiok.pos.domain.model.PcEcrProtocol
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

    fun decodeAll(bytes: ByteArray): Result<List<Arcus2BinLenFrame>> = runCatching {
        val frames = mutableListOf<Arcus2BinLenFrame>()
        var offset = 0
        while (offset < bytes.size) {
            require(bytes.size - offset >= 3) { "Frame is too short at offset=$offset" }
            require(bytes[offset] == SOH) { "Wrong SOH at offset=$offset" }
            val len = ((bytes[offset + 1].toInt() and 0xFF) shl 8) or (bytes[offset + 2].toInt() and 0xFF)
            val frameEnd = offset + 3 + len
            require(frameEnd <= bytes.size) { "Length mismatch at offset=$offset" }
            frames += Arcus2BinLenFrame(bytes.copyOfRange(offset + 3, frameEnd))
            offset = frameEnd
        }
        frames
    }

    fun encode(data: ByteArray): ByteArray {
        require(data.size <= 0xFFFF)
        return byteArrayOf(SOH, ((data.size shr 8) and 0xFF).toByte(), (data.size and 0xFF).toByte()) + data
    }
}

fun ByteArray.toHexPreview(limit: Int = 128): String = take(limit).joinToString(" ") { "%02X".format(it) }
fun decodeWin1251(data: ByteArray): String = data.toString(WIN1251)
fun encodeWin1251(text: String): ByteArray = text.toByteArray(WIN1251)

sealed interface EcrParseResult {
    data class Command(val command: PcEcrCommand) : EcrParseResult
    data class Ack(val bytesToSend: ByteArray? = null) : EcrParseResult
    data class Unknown(val reason: String, val rawHexPreview: String) : EcrParseResult
    data class Error(val reason: String, val bytesToSend: ByteArray? = null) : EcrParseResult
}

interface EcrProtocolAdapter { val protocol: PcEcrProtocol; fun parseIncoming(bytes: ByteArray): EcrParseResult }

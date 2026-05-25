package com.chaiok.pos.data.ecr

import android.content.Context
import android.util.Log
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrCommand
import com.chaiok.pos.domain.model.PcEcrProtocol
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

fun interface Arcus2NewWaySettingsProvider { fun get(): Arcus2NewWaySettings }

class Arcus2RawFrameLogger(private val context: Context) {
    private val dir = File(context.filesDir, "ecr_arcus2_raw").apply { mkdirs() }
    fun logIncoming(bytes: ByteArray) = log("IN", bytes, "incoming")
    fun logOutgoing(bytes: ByteArray, note: String) = log("OUT", bytes, note)
    private fun log(direction: String, bytes: ByteArray, note: String) {
        Log.i("Arcus2Raw", "ARCUS2 $direction length=${bytes.size} hexPreview=${bytes.toHexPreview(32)}")
        runCatching {
            val f = File(dir, "arcus2_raw_${LocalDate.now()}.jsonl")
            f.appendText("{\"direction\":\"$direction\",\"length\":${bytes.size},\"hexPreview\":\"${bytes.toHexPreview(64)}\",\"note\":\"$note\"}\n")
        }
    }
}

class Arcus2NewWayProtocolAdapter(private val settingsProvider: Arcus2NewWaySettingsProvider, private val rawLogger: Arcus2RawFrameLogger) : EcrProtocolAdapter {
    override val protocol: PcEcrProtocol = PcEcrProtocol.ARCUS2_NEWWAY
    override fun parseIncoming(bytes: ByteArray): EcrParseResult {
        rawLogger.logIncoming(bytes)
        val frame = Arcus2BinLenCodec.decode(bytes).getOrElse { return EcrParseResult.Error(it.message ?: "decode") }
        val fields = decodeWin1251(frame.data).trimEnd('\u0000').split('\u001B')
        val cls = fields.getOrNull(0).orEmpty(); val op = fields.getOrNull(1).orEmpty(); val cur = fields.getOrNull(2); val amt = fields.getOrNull(3)
        val s = settingsProvider.get()
        val currency = when (cur) { s.currencyRubCode -> "RUB"; s.currencyAmdCode -> "AMD"; else -> null }
        return when {
            cls == s.saleClass && op == s.saleOp -> amt?.toBigDecimalOrNull()?.let { a -> currency?.let { c -> EcrParseResult.Command(PcEcrCommand.Payment("ARCUS2-SALE-${System.currentTimeMillis()}", protocol, null, a, c)) } } ?: EcrParseResult.Error("sale parse failed")
            cls == s.pingClass && op == s.pingOp -> EcrParseResult.Command(PcEcrCommand.Ping(null, protocol))
            cls == s.settlementClass && op == s.settlementOp -> EcrParseResult.Command(PcEcrCommand.Settlement(null, protocol))
            cls == s.universalReversalClass && op == s.universalReversalOp -> EcrParseResult.Command(PcEcrCommand.Reversal(null, protocol, null, null, amt?.toBigDecimalOrNull(), currency))
            cls == s.refundClass && op == s.refundOp -> EcrParseResult.Command(PcEcrCommand.Refund(null, protocol, amt?.toBigDecimalOrNull(), currency))
            else -> EcrParseResult.Unknown("Unsupported class/op", bytes.toHexPreview())
        }
    }
}

class Arcus2CashRegisterSession(private val client: XchengWireEcrPortClient, private val rawLogger: Arcus2RawFrameLogger, private val settings: Arcus2NewWaySettings) {
    suspend fun sendCommandAndWaitOk(dataText: String): Result<Unit> = sendDataAndWaitOk(encodeWin1251(dataText), dataText.take(24))
    suspend fun sendDataAndWaitOk(data: ByteArray, label: String): Result<Unit> {
        val frame = Arcus2BinLenCodec.encode(data)
        rawLogger.logOutgoing(frame, label)
        client.send(frame).getOrElse { return Result.failure(it) }
        val response = client.receiveOnce().getOrElse { return Result.failure(it) }
        val text = decodeWin1251(Arcus2BinLenCodec.decode(response).getOrElse { return Result.failure(it) }.data).trim('\u0000', ' ', '\n', '\r', '\t')
        return if (text == "OK") Result.success(Unit) else Result.failure(IllegalStateException(text.ifBlank { "no OK" }))
    }
    suspend fun sendPrintReceipt(receipt: String): Result<Unit> {
        val normalized = receipt.replace("\r\n", "\n").replace("\r", "\n")
        var chunk = StringBuilder()
        for (line in normalized.lines()) {
            val candidate = if (chunk.isEmpty()) line else chunk.toString() + "\n" + line
            if (encodeWin1251(candidate).size > settings.maxReceiptPrintBlockBytes && chunk.isNotEmpty()) {
                sendCommandAndWaitOk("PRINT:${chunk}").getOrElse { return Result.failure(it) }
                chunk = StringBuilder(line)
            } else chunk = StringBuilder(candidate)
        }
        if (chunk.isNotEmpty()) sendCommandAndWaitOk("PRINT:${chunk}").getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }
    suspend fun sendSetTags(tags: ByteArray): Result<Unit> = sendDataAndWaitOk(encodeWin1251("SETTAGS:") + tags, "SETTAGS")
}

object Arcus2TagsBuilder { fun buildPaymentTags(result: com.chaiok.pos.domain.model.PcEcrFinalPaymentResult, amount: BigDecimal?, currency: String?, terminalId: String?): ByteArray = ByteArray(0) }

package com.chaiok.pos.data.ecr

import android.content.Context
import android.util.Log
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrCommand
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.model.PcPaymentCommand
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

fun interface Arcus2NewWaySettingsProvider { fun get(): Arcus2NewWaySettings }

interface Arcus2FrameLogger {
    fun logIncoming(bytes: ByteArray)
    fun logOutgoing(bytes: ByteArray, note: String)
}

class NoOpArcus2FrameLogger : Arcus2FrameLogger {
    override fun logIncoming(bytes: ByteArray) = Unit
    override fun logOutgoing(bytes: ByteArray, note: String) = Unit
}

class Arcus2RawFrameLogger(
    private val context: Context,
    private val enableFullRawLog: Boolean = true
) : Arcus2FrameLogger {
    private val dir = File(context.filesDir, "ecr_arcus2_raw").apply { mkdirs() }

    override fun logIncoming(bytes: ByteArray) {
        Log.i("Arcus2Raw", "ARCUS2 IN length=${bytes.size} hexPreview=${bytes.toHexPreview(48)}")
        write("IN", bytes, "incoming")
    }

    override fun logOutgoing(bytes: ByteArray, note: String) {
        Log.i("Arcus2Raw", "ARCUS2 OUT label=$note length=${bytes.size}")
        write("OUT", bytes, note)
    }

    private fun write(direction: String, bytes: ByteArray, note: String) {
        runCatching {
            val obj = JSONObject()
                .put("receivedAt", Instant.now().toString())
                .put("direction", direction)
                .put("length", bytes.size)
                .put("hexPreview", bytes.toHexPreview(96))
                .put("win1251Preview", sanitize(decodeWin1251(bytes).take(256)))
                .put("asciiPreview", sanitize(bytes.toString(Charsets.US_ASCII).take(256)))
                .put("note", sanitize(note))
            if (enableFullRawLog) obj.put("fullHex", bytes.toHexPreview(bytes.size))
            File(dir, "arcus2_raw_${LocalDate.now()}.jsonl").appendText(obj.toString() + "\n")
        }
    }

    private fun sanitize(text: String): String {
        var r = text.replace(Regex("(\\d{6})\\d{3,7}(\\d{4})"), "$1******$2")
        r = r.replace(Regex(";\\d{12,19}="), ";******=")
        return r
    }
}

class Arcus2NewWayProtocolAdapter(
    private val settingsProvider: Arcus2NewWaySettingsProvider,
    private val rawLogger: Arcus2FrameLogger
) : EcrProtocolAdapter {
    override val protocol: PcEcrProtocol = PcEcrProtocol.ARCUS2_NEWWAY
    override fun parseIncoming(bytes: ByteArray): EcrParseResult {
        rawLogger.logIncoming(bytes)
        val frame = Arcus2BinLenCodec.decode(bytes).getOrElse { return EcrParseResult.Error(it.message ?: "decode") }
        val text = decodeWin1251(frame.data).trimEnd('\u0000')
        if (text.startsWith("ENC:")) return EcrParseResult.Error("ARCUS2 ENC encryption is not supported")
        if (text.startsWith("CHUNK:")) return EcrParseResult.Error("ARCUS2 CHUNK is not supported in MVP")

        val fields = text.split('\u001B')
        val cls = fields.getOrNull(0).orEmpty()
        val op = fields.getOrNull(1).orEmpty()
        val currencyCode = fields.getOrNull(2)
        val amountRaw = fields.getOrNull(3)
        val s = settingsProvider.get()
        val currency = when (currencyCode) {
            s.currencyRubCode -> "RUB"
            s.currencyAmdCode -> "AMD"
            else -> null
        }

        return when {
            cls == s.saleClass && op == s.saleOp -> {
                val amount = amountRaw?.toBigDecimalOrNull()
                if (amount == null || currency == null) EcrParseResult.Error("sale parse failed")
                else EcrParseResult.Command(PcEcrCommand.Payment("ARCUS2-SALE-${System.currentTimeMillis()}", protocol, null, amount, currency))
            }
            cls == s.pingClass && op == s.pingOp -> EcrParseResult.Command(PcEcrCommand.Ping(null, protocol))
            cls == s.settlementClass && op == s.settlementOp -> EcrParseResult.Command(PcEcrCommand.Settlement(null, protocol))
            cls == s.universalReversalClass && op == s.universalReversalOp -> EcrParseResult.Command(PcEcrCommand.Reversal(null, protocol, null, null, amountRaw?.toBigDecimalOrNull(), currency))
            cls == s.refundClass && op == s.refundOp -> EcrParseResult.Command(PcEcrCommand.Refund(null, protocol, amountRaw?.toBigDecimalOrNull(), currency))
            else -> EcrParseResult.Unknown("Unsupported class/op", bytes.toHexPreview())
        }
    }
}

class Arcus2CashRegisterSession(
    private val client: XchengWireEcrPortClient,
    private val rawLogger: Arcus2FrameLogger,
    private val settings: Arcus2NewWaySettings
) {
    suspend fun sendCommandAndWaitOk(dataText: String): Result<Unit> = sendDataAndWaitOk(encodeWin1251(dataText), dataText.substringBefore(':'))

    suspend fun sendDataAndWaitOk(data: ByteArray, label: String): Result<Unit> {
        val frame = Arcus2BinLenCodec.encode(data)
        rawLogger.logOutgoing(frame, label)
        client.send(frame).getOrElse { return Result.failure(it) }
        val response = client.receiveOnce().getOrElse { return Result.failure(it) }
        val responseText = decodeWin1251(Arcus2BinLenCodec.decode(response).getOrElse { return Result.failure(it) }.data)
            .trim('\u0000', ' ', '\n', '\r', '\t')

        return when (responseText) {
            "OK" -> Result.success(Unit)
            "ER" -> Result.failure(IllegalStateException("Cash register returned ER for $label"))
            else -> Result.failure(IllegalStateException("Unexpected ARCUS2 response for $label: ${responseText.take(32)}"))
        }
    }

    suspend fun sendPrintReceipt(receipt: String): Result<Unit> {
        val normalized = receipt.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalized.lines()
        val chunk = StringBuilder()
        for (line in lines) {
            val candidate = if (chunk.isEmpty()) line else "${chunk}\n$line"
            if (encodeWin1251(candidate).size > settings.maxReceiptPrintBlockBytes && chunk.isNotEmpty()) {
                sendCommandAndWaitOk("PRINT:$chunk").getOrElse { return Result.failure(it) }
                chunk.clear(); chunk.append(line)
            } else {
                chunk.clear(); chunk.append(candidate)
            }
        }
        if (chunk.isNotEmpty()) sendCommandAndWaitOk("PRINT:$chunk").getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }
}

object Arcus2TagsBuilder {
    fun buildPaymentTags(result: PcEcrFinalPaymentResult, amount: BigDecimal?, currency: String?, terminalId: String?): ByteArray = ByteArray(0)
}

data class Arcus2OutgoingCommand(val label: String, val data: ByteArray)

object Arcus2NewWayResultSequenceBuilder {
    fun buildPaymentResultSequence(sourceCommand: PcPaymentCommand, result: PcEcrFinalPaymentResult, receiptText: String?, settings: Arcus2NewWaySettings): List<Arcus2OutgoingCommand> {
        val commands = mutableListOf<Arcus2OutgoingCommand>()
        fun addText(label: String, text: String) { commands += Arcus2OutgoingCommand(label, encodeWin1251(text)) }

        when (result) {
            is PcEcrFinalPaymentResult.Approved -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Одобрено")
                if (settings.sendPrintCommands && !receiptText.isNullOrBlank()) {
                    if (settings.sendStartEndPrint) addText("STARTPRINT", "STARTPRINT:CUSTOMER")
                    addText("PRINT", "PRINT:${receiptText.replace("\r\n", "\n").replace("\r", "\n")}")
                    if (settings.sendStartEndPrint) addText("ENDPRINT", "ENDPRINT:CUSTOMER")
                }
                addText("STORERC", "STORERC:00")
                if (settings.sendSetTags) commands += Arcus2OutgoingCommand("SETTAGS", encodeWin1251("SETTAGS:") + Arcus2TagsBuilder.buildPaymentTags(result, sourceCommand.amount, sourceCommand.currency, null))
                addText("ENDTR", "ENDTR")
            }
            is PcEcrFinalPaymentResult.Declined -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Отклонено")
                addText("STORERC", "STORERC:${result.resultCode ?: settings.declinedDefaultRc}")
                if (settings.sendSetTags) addText("SETTAGS", "SETTAGS:")
                addText("ENDTR", "ENDTR")
            }
            is PcEcrFinalPaymentResult.Cancelled -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Отменено")
                addText("STORERC", "STORERC:${settings.cancelledRc}")
                addText("ENDTR", "ENDTR")
            }
            is PcEcrFinalPaymentResult.Error -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Ошибка")
                addText("STORERC", "STORERC:${settings.errorRc}")
                addText("ENDTR", "ENDTR")
            }
        }
        return commands
    }
}

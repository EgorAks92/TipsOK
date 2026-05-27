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
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    private val enableFullRawLog: Boolean = false
) : Arcus2FrameLogger {
    private val dir = File(context.filesDir, "ecr_arcus2_raw").apply { mkdirs() }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun logIncoming(bytes: ByteArray) {
        Log.i("Arcus2Raw", "ARCUS2 IN length=${bytes.size} hexPreview=${bytes.toHexPreview(48)}")
        ioScope.launch { write("IN", bytes, "incoming") }
    }

    override fun logOutgoing(bytes: ByteArray, note: String) {
        Log.i("Arcus2Raw", "ARCUS2 OUT label=$note length=${bytes.size}")
        ioScope.launch { write("OUT", bytes, note) }
    }

    private fun write(direction: String, bytes: ByteArray, note: String) {
        runCatching {
            val obj = JSONObject()
                .put("receivedAt", Instant.now().toString())
                .put("direction", direction)
                .put("length", bytes.size)
                .put("hexPreview", bytes.toHexPreview(96))
                .put("win1251Preview", sanitize(extractWin1251Preview(bytes)))
                .put("asciiPreview", sanitize(extractAsciiPreview(bytes)))
                .put("note", sanitize(note))
            if (enableFullRawLog) obj.put("fullHex", bytes.toHexPreview(bytes.size))
            File(dir, "arcus2_raw_${LocalDate.now()}.jsonl").appendText(obj.toString() + "\n")
        }
    }

    private fun extractWin1251Preview(bytes: ByteArray): String {
        val payload = Arcus2BinLenCodec.decode(bytes).getOrNull()?.data ?: bytes
        return decodeWin1251(payload).take(256)
    }

    private fun extractAsciiPreview(bytes: ByteArray): String {
        val payload = Arcus2BinLenCodec.decode(bytes).getOrNull()?.data ?: bytes
        return payload.toString(Charsets.US_ASCII).take(256)
    }

    private fun sanitize(text: String): String {
        var r = text.replace(Regex("(\\d{6})\\d{3,7}(\\d{4})"), "$1******$2")
        r = r.replace(Regex(";\\d{12,19}="), ";******=")
        r = r.replace(Regex("(?i)(cvv|cvc)\\s*[:=]?\\s*\\d{3,4}"), "$1=***")
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
        val text = decodeWin1251(frame.data).trim('\u0000', ' ', '\n', '\r', '\t')
        if (text == "OK" || text == "ER" || text == "NAK") return EcrParseResult.Ack()
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
                val amount = parseArcus2Amount(amountRaw, currency)
                // TODO: Make PcEcrCommand.Payment amount/currency nullable for full primary+additional-data resolution.
                if (amount == null || currency == null) EcrParseResult.Error("sale parse failed")
                else EcrParseResult.Command(PcEcrCommand.Payment("ARCUS2-SALE-${System.currentTimeMillis()}", protocol, null, amount, currency))
            }
            cls == s.pingClass && op == s.pingOp -> EcrParseResult.Command(PcEcrCommand.Ping(null, protocol))
            cls == s.settlementClass && op == s.settlementOp -> EcrParseResult.Command(PcEcrCommand.Settlement(null, protocol))
            cls == s.universalReversalClass && op == s.universalReversalOp -> {
                val rrn = parseArcus2Rrn(fields, currencyCode, amountRaw)
                Log.i("Arcus2Adapter", "reversal fields=${fields.map(::maskArcusField)} rrnMasked=${maskRrn(rrn)}")
                val commandId = if (rrn.isNullOrBlank()) {
                    "ARCUS2-REVERSAL-NO-RRN-${System.currentTimeMillis()}"
                } else {
                    "ARCUS2-REVERSAL-${rrn.takeLast(4)}-${System.currentTimeMillis()}"
                }
                EcrParseResult.Command(PcEcrCommand.Reversal(commandId, protocol, null, rrn, parseArcus2Amount(amountRaw, currency), currency))
            }
            cls == s.refundClass && op == s.refundOp -> EcrParseResult.Command(PcEcrCommand.Refund(null, protocol, amountRaw?.toBigDecimalOrNull(), currency))
            else -> EcrParseResult.Unknown("Unsupported class/op", bytes.toHexPreview())
        }
    }
}

private fun parseArcus2Rrn(fields: List<String>, currencyCode: String?, amountRaw: String?): String? {
    fields.forEachIndexed { index, field ->
        val trimmed = field.trim()
        val lower = trimmed.lowercase()
        if (lower.startsWith("rrn=") || lower.startsWith("r=")) {
            return trimmed.substringAfter('=').trim().takeIf { it.matches(Regex("\\d{6,12}")) }
        }
        if (lower.startsWith("/r")) {
            val candidate = trimmed
                .drop(2)
                .trim()
                .trim('[', ']')
                .filter { it.isDigit() }
            if (candidate.matches(Regex("\\d{6,12}"))) {
                return candidate
            }
        }
        if (trimmed.equals("/r", ignoreCase = true)) {
            val next = fields.getOrNull(index + 1)
                ?.trim()
                ?.trim('[', ']')
                ?.filter { it.isDigit() }
            if (next != null && next.matches(Regex("\\d{6,12}"))) {
                return next
            }
        }
    }
    return fields
        .drop(2)
        .map { it.trim() }
        .firstOrNull {
            it.matches(Regex("\\d{6,12}")) &&
                    it != currencyCode &&
                    it != amountRaw &&
                    it != amountRaw.orEmpty().replace(".", "").replace(",", "")
        }
}

private fun maskRrn(rrn: String?): String =
    rrn?.takeLast(4)?.padStart(rrn.length, '*') ?: "<missing>"

private fun maskArcusField(value: String): String =
    run {
        val trimmed = value.trim()
        val rrnSlash = Regex("(?i)/r\\[?\\d{6,12}\\]?").find(trimmed)?.value
        if (rrnSlash != null) {
            val rrn = rrnSlash
                .removePrefix("/r")
                .removePrefix("/R")
                .trim('[', ']')
            return@run trimmed.replace(rrnSlash, "/r${maskRrn(rrn)}")
        }
        if (trimmed.matches(Regex("\\d{6,19}"))) trimmed.takeLast(4).padStart(trimmed.length, '*')
        else trimmed.take(32)
    }

private fun parseArcus2Amount(raw: String?, currency: String?): BigDecimal? {
    val normalizedRaw = raw?.trim()?.replace(',', '.')?.takeIf { it.isNotBlank() } ?: return null
    val n = normalizedRaw.toBigDecimalOrNull() ?: return null
    val hasDecimalSeparator = normalizedRaw.contains('.')
    return when (currency?.uppercase()) {
        "RUB" -> if (hasDecimalSeparator) n.setScale(2, RoundingMode.HALF_UP) else n.movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        "AMD" -> n.setScale(0, RoundingMode.HALF_UP)
        else -> n
    }
}

class Arcus2CashRegisterSession(
    private val client: XchengWireEcrPortClient,
    private val rawLogger: Arcus2FrameLogger,
    private val settings: Arcus2NewWaySettings
) {
    data class Arcus2ReceivedFrame(
        val data: ByteArray,
        val text: String
    )

    suspend fun sendCommandAndWaitOk(dataText: String): Result<Unit> = sendDataAndWaitOk(encodeWin1251(dataText), dataText.substringBefore(':'))
    suspend fun sendCommandAndRunAdditionalDataSession(
        dataText: String,
        readTimeoutMs: Long,
        maxFrames: Int,
        shouldStop: (List<Arcus2ReceivedFrame>) -> Boolean = { false }
    ): Result<List<Arcus2ReceivedFrame>> = runCatching {
        val outFrame = Arcus2BinLenCodec.encode(encodeWin1251(dataText))
        rawLogger.logOutgoing(outFrame, dataText.substringBefore(':'))
        client.send(outFrame).getOrThrow()

        val responses = mutableListOf<Arcus2ReceivedFrame>()
        var stop = false
        var index = 0
        val limit = maxFrames.coerceAtLeast(1)

        while (index < limit && !stop) {
            val bytes = client.receiveOnce(readTimeoutMs).getOrNull()
            if (bytes != null && bytes.isNotEmpty()) {
                Log.i("Arcus2Session", "ARCUS2 additional data recv bytes=${bytes.size}")
                val frames = Arcus2BinLenCodec.decodeAll(bytes).getOrNull().orEmpty()
                for (frame in frames) {
                    val frameData = frame.data
                    val text = decodeWin1251(frameData).trim('\u0000', ' ', '\n', '\r', '\t')
                    val normalized = text.trim()
                    responses.add(Arcus2ReceivedFrame(data = frameData, text = text))
                    when {
                        normalized.equals("OK", ignoreCase = true) ||
                            normalized.equals("ER", ignoreCase = true) ||
                            normalized.equals("NAK", ignoreCase = true) -> Unit

                        normalized.startsWith("GETFILE:", ignoreCase = true) -> {
                            val fileName = normalized.substringAfter(':', "")
                                .replace("/", "")
                                .replace("\\", "")
                                .take(64)
                            Log.i("Arcus2Session", "ARCUS2 additional GETFILE requested file=$fileName -> ER")
                            sendArcusControlText("ER", "additional-ER")
                        }

                        normalized.startsWith("PING:", ignoreCase = true) -> {
                            Log.i("Arcus2Session", "ARCUS2 additional PING -> OK")
                            sendArcusControlText("OK", "additional-OK")
                        }

                        normalized.startsWith("GETTAGS:", ignoreCase = true) -> {
                            Log.i("Arcus2Session", "ARCUS2 additional GETTAGS command from peer -> ER")
                            sendArcusControlText("ER", "additional-ER")
                        }

                        normalized.startsWith("SETTAGS:", ignoreCase = true) -> {
                            sendArcusControlText("OK", "additional-OK")
                        }

                        normalized.equals("ENDTR", ignoreCase = true) -> {
                            sendArcusControlText("OK", "additional-OK")
                            stop = true
                        }

                        else -> {
                            if (frameData.isNotEmpty()) {
                                sendArcusControlText("OK", "additional-OK")
                            }
                        }
                    }
                }
                stop = stop || shouldStop(responses)
            }
            index += 1
        }

        // TODO: confirm whether Arcus additional data response requires OK ACK.
        responses
    }

    private suspend fun sendArcusControlText(text: String, note: String): Result<Unit> {
        val frame = Arcus2BinLenCodec.encode(encodeWin1251(text))
        rawLogger.logOutgoing(frame, note)
        return client.send(frame)
    }

    suspend fun sendDataAndWaitOk(data: ByteArray, label: String): Result<Unit> {
        val frame = Arcus2BinLenCodec.encode(data)
        rawLogger.logOutgoing(frame, label)
        client.send(frame).getOrElse { return Result.failure(it) }
        Log.i("Arcus2Session", "ARCUS2 OUT sent label=$label bytes=${frame.size}")
        if (!settings.waitOkAfterEachCommand) {
            Log.i("Arcus2Session", "ARCUS2 sent label=$label without waiting OK")
            val drained = client.receiveOnce(settings.drainOkAfterCommandMs).getOrNull()
            val responses = drained
                ?.let { Arcus2BinLenCodec.decodeAll(it).getOrNull() }
                .orEmpty()
                .map { decodeWin1251(it.data).trim('\u0000', ' ', '\n', '\r', '\t') }

            if (responses.isEmpty() || responses.all { it.isBlank() }) return Result.success(Unit)
            return when {
                responses.any { it == "ER" } -> Result.failure(IllegalStateException("Cash register returned ER for $label"))
                responses.any { it == "NAK" } -> Result.failure(IllegalStateException("Cash register returned NAK for $label"))
                responses.any { it == "OK" } -> Result.success(Unit)
                else -> {
                    Log.w("Arcus2Session", "ARCUS2 drain unknown label=$label resp=${responses.joinToString("|").take(32)}")
                    Result.success(Unit)
                }
            }
        }

        Log.i("Arcus2Session", "ARCUS2 wait OK label=$label timeoutMs=${settings.waitOkTimeoutMs}")
        val response = client.receiveOnce(settings.waitOkTimeoutMs)
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(IllegalStateException("ARCUS2 cash register OK timeout for $label").also { Log.w("Arcus2Session", "ARCUS2 OK timeout label=$label") })
        val responses = Arcus2BinLenCodec.decodeAll(response).getOrElse { return Result.failure(it) }
            .map { decodeWin1251(it.data).trim('\u0000', ' ', '\n', '\r', '\t') }

        return when {
            responses.any { it == "ER" } -> Result.failure(IllegalStateException("Cash register returned ER for $label"))
            responses.any { it == "NAK" } -> Result.failure(IllegalStateException("Cash register returned NAK for $label"))
            responses.any { it == "OK" } -> { Log.i("Arcus2Session", "ARCUS2 OK label=$label"); Result.success(Unit) }
            else -> Result.failure(IllegalStateException("Unexpected ARCUS2 response for $label: ${responses.joinToString("|").take(32)}"))
        }
    }
}

object Arcus2TagsBuilder {
    private const val ESC = '\u001B'

    fun buildPaymentTags(data: Arcus2PaymentTagData): ByteArray {
        val amount = formatAmount(data.amount, data.currency)
        val pairs = linkedMapOf(
            "RC" to data.responseCode,
            "RRN" to data.rrn,
            "AUTH" to data.authCode,
            "TERM" to data.terminalId,
            "EXTID" to data.externalTransactionId,
            "RECEIPT" to data.receiptNumber,
            "AMOUNT" to amount,
            "CURRENCY" to data.currency,
            "STATUS" to data.status
        )
        val sanitizedPairs = pairs
            .mapNotNull { (key, value) ->
                val sanitized = sanitizeTagValue(value)
                if (sanitized.isNullOrBlank()) null else key to sanitized
            }
        val payload = sanitizedPairs.joinToString(ESC.toString()) { "${it.first}=${it.second}" }
        val payloadBytes = if (payload.isBlank()) ByteArray(0) else encodeWin1251(payload)
        Log.i("Arcus2Tags", "SETTAGS built keys=${sanitizedPairs.joinToString(",") { it.first }} bytes=${payloadBytes.size}")
        return payloadBytes
    }

    private fun formatAmount(amount: BigDecimal?, currency: String?): String? {
        if (amount == null) return null
        val scale = if (currency.equals("AMD", true)) 0 else 2
        return amount.setScale(scale, java.math.RoundingMode.HALF_UP).toPlainString()
    }

    private fun sanitizeTagValue(value: String?): String? {
        if (value == null) return null
        val compact = value.trim()
            .replace(ESC, ' ')
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .replace(Regex("\\s+"), " ")
            .take(64)
        return compact.replace(Regex("\\b(\\d{6})(\\d{3,9})(\\d{4})\\b"), "$1******$3")
    }
}

data class Arcus2PaymentTagData(
    val responseCode: String?,
    val rrn: String?,
    val authCode: String?,
    val terminalId: String?,
    val externalTransactionId: String?,
    val receiptNumber: String?,
    val amount: BigDecimal?,
    val currency: String?,
    val status: String
)

data class Arcus2OutgoingCommand(val label: String, val data: ByteArray, val critical: Boolean = true)

object Arcus2NewWayResultSequenceBuilder {
    fun buildPaymentResultSequence(
        sourceCommand: PcPaymentCommand,
        result: PcEcrFinalPaymentResult,
        receiptText: String?,
        settings: Arcus2NewWaySettings,
        terminalId: String? = null
    ): List<Arcus2OutgoingCommand> {
        val commands = mutableListOf<Arcus2OutgoingCommand>()
        fun addText(label: String, text: String, critical: Boolean = true) { commands += Arcus2OutgoingCommand(label, encodeWin1251(text), critical) }

        if (settings.minimalResultMode) {
            val shouldPrintReceipt = settings.sendReceiptInMinimalMode && settings.sendPrintCommands && !receiptText.isNullOrBlank()
            if (shouldPrintReceipt) {
                if (settings.usePrintSessionMarkersInMinimalMode) addText("STARTPRINT", "STARTPRINT:CUSTOMER", critical = false)
                splitReceiptToPrintChunks(receiptText.orEmpty(), settings.maxReceiptPrintBlockBytes).forEach { chunk ->
                    if (chunk.isNotBlank()) addText("PRINT", "PRINT:$chunk", critical = false)
                }
                if (settings.usePrintSessionMarkersInMinimalMode) addText("ENDPRINT", "ENDPRINT:CUSTOMER", critical = false)
            }
            when (result) {
                is PcEcrFinalPaymentResult.Approved -> addText("STORERC", "STORERC:00")
                is PcEcrFinalPaymentResult.Declined -> addText("STORERC", "STORERC:${result.resultCode ?: settings.declinedDefaultRc}")
                is PcEcrFinalPaymentResult.Cancelled -> addText("STORERC", "STORERC:${settings.cancelledRc}")
                is PcEcrFinalPaymentResult.Error -> addText("STORERC", "STORERC:${settings.errorRc}")
            }
            val tagData = buildTagData(sourceCommand, result, settings, terminalId)
            commands += Arcus2OutgoingCommand("SETTAGS", encodeWin1251("SETTAGS:") + Arcus2TagsBuilder.buildPaymentTags(tagData))
            addText("ENDTR", "ENDTR")
            return commands
        }


        when (result) {
            is PcEcrFinalPaymentResult.Approved -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Одобрено")
                if (settings.sendPrintCommands && !receiptText.isNullOrBlank()) {
                    if (settings.sendStartEndPrint) addText("STARTPRINT", "STARTPRINT:CUSTOMER", critical = false)
                    splitReceiptToPrintChunks(receiptText, settings.maxReceiptPrintBlockBytes).forEach { chunk ->
                        addText("PRINT", "PRINT:$chunk", critical = false)
                    }
                    if (settings.sendStartEndPrint) addText("ENDPRINT", "ENDPRINT:CUSTOMER", critical = false)
                }
                addText("STORERC", "STORERC:00")
                if (settings.sendSetTags) {
                    val tagData = buildTagData(sourceCommand, result, settings, terminalId)
                    commands += Arcus2OutgoingCommand("SETTAGS", encodeWin1251("SETTAGS:") + Arcus2TagsBuilder.buildPaymentTags(tagData))
                }
                addText("ENDTR", "ENDTR")
            }
            is PcEcrFinalPaymentResult.Declined -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Отклонено")
                if (settings.sendPrintCommands && !receiptText.isNullOrBlank()) {
                    if (settings.sendStartEndPrint) addText("STARTPRINT", "STARTPRINT:CUSTOMER", critical = false)
                    splitReceiptToPrintChunks(receiptText, settings.maxReceiptPrintBlockBytes).forEach { chunk ->
                        addText("PRINT", "PRINT:$chunk", critical = false)
                    }
                    if (settings.sendStartEndPrint) addText("ENDPRINT", "ENDPRINT:CUSTOMER", critical = false)
                }
                addText("STORERC", "STORERC:${result.resultCode ?: settings.declinedDefaultRc}")
                if (settings.sendSetTags) {
                    val tagData = buildTagData(sourceCommand, result, settings, terminalId)
                    commands += Arcus2OutgoingCommand("SETTAGS", encodeWin1251("SETTAGS:") + Arcus2TagsBuilder.buildPaymentTags(tagData))
                }
                addText("ENDTR", "ENDTR")
            }
            is PcEcrFinalPaymentResult.Cancelled -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Отменено")
                addText("STORERC", "STORERC:${settings.cancelledRc}")
                if (settings.sendSetTags) {
                    val tagData = buildTagData(sourceCommand, result, settings, terminalId)
                    commands += Arcus2OutgoingCommand("SETTAGS", encodeWin1251("SETTAGS:") + Arcus2TagsBuilder.buildPaymentTags(tagData))
                }
                addText("ENDTR", "ENDTR")
            }
            is PcEcrFinalPaymentResult.Error -> {
                if (settings.sendStatusMessages) addText("STATUS", "STATUS:Ошибка")
                addText("STORERC", "STORERC:${settings.errorRc}")
                if (settings.sendSetTags) {
                    val tagData = buildTagData(sourceCommand, result, settings, terminalId)
                    commands += Arcus2OutgoingCommand("SETTAGS", encodeWin1251("SETTAGS:") + Arcus2TagsBuilder.buildPaymentTags(tagData))
                }
                addText("ENDTR", "ENDTR")
            }
        }
        return commands
    }

    private fun buildTagData(
        sourceCommand: PcPaymentCommand,
        result: PcEcrFinalPaymentResult,
        settings: Arcus2NewWaySettings,
        terminalId: String?
    ): Arcus2PaymentTagData = when (result) {
        is PcEcrFinalPaymentResult.Approved -> Arcus2PaymentTagData(
            responseCode = result.resultCode ?: "00",
            rrn = result.rrn,
            authCode = result.authCode,
            terminalId = terminalId,
            externalTransactionId = result.externalTransactionId,
            receiptNumber = sourceCommand.orderId,
            amount = sourceCommand.amount,
            currency = sourceCommand.currency,
            status = "approved"
        )
        is PcEcrFinalPaymentResult.Declined -> Arcus2PaymentTagData(result.resultCode ?: "05", null, null, terminalId, null, sourceCommand.orderId, sourceCommand.amount, sourceCommand.currency, "declined")
        is PcEcrFinalPaymentResult.Cancelled -> Arcus2PaymentTagData(settings.cancelledRc, null, null, terminalId, null, sourceCommand.orderId, sourceCommand.amount, sourceCommand.currency, "cancelled")
        is PcEcrFinalPaymentResult.Error -> Arcus2PaymentTagData(settings.errorRc, null, null, terminalId, null, sourceCommand.orderId, sourceCommand.amount, sourceCommand.currency, "error")
    }

    private fun splitReceiptToPrintChunks(receipt: String, maxBytes: Int): List<String> {
        val normalized = receipt.replace("\r\n", "\n").replace("\r", "\n")
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                chunks += current.toString()
                current = StringBuilder()
            }
        }

        for (line in normalized.lines()) {
            val candidate = if (current.isEmpty()) line else "$current\n$line"
            if (encodeWin1251("PRINT:$candidate").size <= maxBytes) {
                current = StringBuilder(candidate)
            } else {
                flush()
                if (encodeWin1251("PRINT:$line").size <= maxBytes) {
                    current.append(line)
                } else {
                    var part = StringBuilder()
                    for (ch in line) {
                        val test = part.toString() + ch
                        if (encodeWin1251("PRINT:$test").size > maxBytes && part.isNotEmpty()) {
                            chunks += part.toString()
                            part = StringBuilder(ch.toString())
                        } else {
                            part.append(ch)
                        }
                    }
                    if (part.isNotEmpty()) chunks += part.toString()
                }
            }
        }
        flush()
        return chunks.filter { it.isNotBlank() }
    }
}

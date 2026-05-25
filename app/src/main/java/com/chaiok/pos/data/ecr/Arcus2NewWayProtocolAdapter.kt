package com.chaiok.pos.data.ecr

import android.util.Log
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrProtocol
import java.math.BigDecimal

fun interface Arcus2NewWaySettingsProvider { fun get(): Arcus2NewWaySettings }

class Arcus2RawFrameLogger {
    fun logIncoming(bytes: ByteArray) { Log.i("Arcus2Raw", "ARCUS2 IN length=${bytes.size} hexPreview=${bytes.toHexPreview(32)}") }
    fun logOutgoing(bytes: ByteArray, note: String) { Log.i("Arcus2Raw", "ARCUS2 OUT length=${bytes.size} note=$note") }
}

class Arcus2NewWayProtocolAdapter(
    private val settingsProvider: Arcus2NewWaySettingsProvider,
    private val rawLogger: Arcus2RawFrameLogger
) : EcrProtocolAdapter {
    override val protocol: PcEcrProtocol = PcEcrProtocol.ARCUS2_NEWWAY

    override fun parseIncoming(bytes: ByteArray): EcrParseResult {
        rawLogger.logIncoming(bytes)
        val frame = Arcus2BinLenCodec.decode(bytes).getOrElse { return EcrParseResult.Error(it.message ?: "decode") }
        val dataText = decodeWin1251(frame.data).trimEnd('\u0000')
        if (dataText.startsWith("ENC:")) return EcrParseResult.Error("ARCUS2 ENC encryption is not supported")
        if (dataText.startsWith("CHUNK:")) return EcrParseResult.Error("ARCUS2 CHUNK is not supported in MVP")
        val fields = dataText.split('\u001B')
        val operationClass = fields.getOrNull(0).orEmpty()
        val operationCode = fields.getOrNull(1).orEmpty()
        val currencyCode = fields.getOrNull(2)
        val amountRaw = fields.getOrNull(3)
        val settings = settingsProvider.get()
        val currency = when (currencyCode) { settings.currencyRubCode -> "RUB"; settings.currencyAmdCode -> "AMD"; else -> null }

        return when {
            operationClass == settings.saleClass && operationCode == settings.saleOp -> {
                val amount = amountRaw?.toBigDecimalOrNull()
                if (amount == null || currency == null) EcrParseResult.Error("sale parse failed") else EcrParseResult.Command(PcEcrCommand.Payment("ARCUS2-SALE-${System.currentTimeMillis()}", protocol, null, amount, currency))
            }
            operationClass == settings.settlementClass && operationCode == settings.settlementOp -> EcrParseResult.Command(PcEcrCommand.Settlement(null, protocol))
            operationClass == settings.pingClass && operationCode == settings.pingOp -> EcrParseResult.Command(PcEcrCommand.Ping(null, protocol))
            operationClass == settings.universalReversalClass && operationCode == settings.universalReversalOp -> EcrParseResult.Command(PcEcrCommand.Reversal(null, protocol, null, null, amountRaw?.toBigDecimalOrNull(), currency))
            operationClass == settings.refundClass && operationCode == settings.refundOp -> EcrParseResult.Command(PcEcrCommand.Refund(null, protocol, amountRaw?.toBigDecimalOrNull(), currency))
            else -> EcrParseResult.Unknown("Unsupported class/op", bytes.toHexPreview())
        }
    }
}

class Arcus2CashRegisterSession(
    private val client: XchengWireEcrPortClient,
    private val rawLogger: Arcus2RawFrameLogger,
    private val settingsProvider: Arcus2NewWaySettingsProvider
) {
    suspend fun sendCommandAndWaitOk(dataText: String): Result<Unit> = sendDataAndWaitOk(encodeWin1251(dataText), dataText.take(32))
    suspend fun sendDataAndWaitOk(data: ByteArray, label: String): Result<Unit> {
        val frame = Arcus2BinLenCodec.encode(data)
        rawLogger.logOutgoing(frame, label)
        client.send(frame).getOrElse { return Result.failure(it) }
        val response = client.receiveOnce().getOrElse { return Result.failure(it) }
        val text = decodeWin1251(Arcus2BinLenCodec.decode(response).getOrElse { return Result.failure(it) }.data)
        return when (text) { "OK" -> Result.success(Unit); "ER" -> Result.failure(IllegalStateException("ER")); else -> Result.failure(IllegalStateException("Unexpected: $text")) }
    }
}

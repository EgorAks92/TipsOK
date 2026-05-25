package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.PcPaymentCommand
import org.json.JSONObject
import java.math.BigDecimal
import com.chaiok.pos.domain.model.PcEcrProtocol
import java.math.RoundingMode

object PcPaymentCommandParser {
    // TODO: replace parser with final approved PC/ECR protocol details if changed.
    fun parse(bytes: ByteArray): PcPaymentCommand? {
        val normalized = bytes.toString(Charsets.UTF_8)
            .replace("\u0000", "")
            .trim()
        if (normalized.isBlank()) return null

        normalized.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                parseJsonLine(line)?.let { return it }
            }

        return parseFallback(normalized)
    }

    private fun parseJsonLine(line: String): PcPaymentCommand? = runCatching {
        val obj = JSONObject(line)
        if (obj.optString("proto") != "chaiok-ecr") return null
        if (obj.optInt("version", -1) != 1) return null
        if (obj.optString("type") != "payment") return null

        val currency = obj.optString("currency", "RUB")
            .trim()
            .uppercase()
            .ifBlank { "RUB" }
        if (currency !in setOf("RUB", "AMD")) return null

        val amountRaw: String = when (val amount = obj.opt("amount")) {
            is Number -> amount.toString()
            is String -> amount
            else -> return null
        }

        val amount = parseAmount(amountRaw, currency = currency, allowComma = false) ?: return null
        PcPaymentCommand(
            amount = amount,
            commandId = obj.optString("commandId").ifBlank { null },
            orderId = obj.optString("orderId").ifBlank { null },
            currency = currency,
            rawPayloadPreview = preview(line),
            sourceProtocol = PcEcrProtocol.CHAIOK_JSON
        )
    }.getOrNull()

    private fun parseFallback(text: String): PcPaymentCommand? {
        val token = when {
            text.startsWith("PAY ", true) -> text.substringAfter(' ')
            text.startsWith("AMOUNT=", true) -> text.substringAfter('=')
            else -> text
        }
        val amount = parseAmount(token, currency = "RUB", allowComma = true) ?: return null
        return PcPaymentCommand(amount = amount, rawPayloadPreview = preview(text), sourceProtocol = PcEcrProtocol.CHAIOK_JSON)
    }

    private fun parseAmount(raw: String, currency: String, allowComma: Boolean): BigDecimal? {
        val value = raw.trim().let { if (allowComma) it.replace(',', '.') else it }
        val decimal = value.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
        return when (currency.uppercase()) {
            "AMD" -> decimal.setScale(0, RoundingMode.HALF_UP)
            else -> decimal.setScale(2, RoundingMode.HALF_UP)
        }
    }

    private fun preview(value: String): String = value
        .replace(Regex("\\s+"), " ")
        .take(120)
}

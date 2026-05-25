package com.chaiok.pos.data.ecr

object PcEcrSafeLogSanitizer {
    private val panRegex = Regex("(?<!\\d)(\\d{13,19})(?!\\d)")
    private val cvvRegex = Regex("(?i)(cvv|cvc)\\s*[:=]?\\s*\\d{3,4}")
    private val track2Regex = Regex(";?[0-9]{12,19}=[0-9]{1,}\\??")

    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        return raw
            .replace(track2Regex, "[TRACK2_MASKED]")
            .replace(cvvRegex, "$1=[CVV_MASKED]")
            .replace(panRegex) { match ->
                val value = match.value
                if (value.length < 10) return@replace value
                value.take(4) + "*".repeat(value.length - 8) + value.takeLast(4)
            }
    }
}


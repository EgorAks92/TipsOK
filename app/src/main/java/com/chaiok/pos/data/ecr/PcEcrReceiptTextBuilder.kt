package com.chaiok.pos.data.ecr

class PcEcrReceiptTextBuilder {
    fun buildFromSspReceipt(rawReceipt: String): String =
        normalizeAndSanitize(rawReceipt).ifBlank { "ЧАЙОК / TIPLY\nРЕЗУЛЬТАТ ОПЕРАЦИИ" }

    fun buildFallbackReceipt(frame: ChaiOkEcrPaymentResultFrame): String {
        val text = buildString {
            appendLine("ЧАЙОК / TIPLY")
            appendLine("РЕЗУЛЬТАТ ОПЕРАЦИИ")
            appendLine()
            appendLine("Статус: ${frame.status}")
            appendLine("Сообщение: ${frame.message.orEmpty()}")
            appendLine("Код результата: ${frame.resultCode.orEmpty()}")
            appendLine()
            appendLine("Command ID: ${frame.commandId}")
            appendLine("Order ID: ${frame.orderId.orEmpty()}")
            appendLine("Terminal ID: ${frame.terminalId.orEmpty()}")
            appendLine("External transaction ID: ${frame.externalTransactionId.orEmpty()}")
            appendLine("RRN: ${frame.rrn.orEmpty()}")
            appendLine("Auth code: ${frame.authCode.orEmpty()}")
            appendLine()
            appendLine("Сумма счета: ${frame.billAmount.orEmpty()} ${frame.currency}")
            appendLine("Чаевые: ${frame.tipAmount.orEmpty()} ${frame.currency}")
            appendLine("Итого: ${frame.totalAmount.orEmpty()} ${frame.currency}")
            appendLine()
            appendLine("Дата: ${frame.createdAt}")
        }
        return normalizeAndSanitize(text)
    }

    private fun normalizeAndSanitize(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\u0000', '\n', ' ')
        return PcEcrSafeLogSanitizer.sanitize(normalized).orEmpty()
    }
}


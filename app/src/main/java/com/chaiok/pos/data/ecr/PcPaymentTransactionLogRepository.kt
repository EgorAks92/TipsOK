package com.chaiok.pos.data.ecr

import android.content.Context
import org.json.JSONObject
import java.io.File

class PcPaymentTransactionLogRepository(context: Context) {
    private val file = File(context.filesDir, "pc_transactions.jsonl")

    fun save(frame: ChaiOkEcrPaymentResultFrame, sendStatus: String, sendError: String?) {
        val json = JSONObject()
            .put("id", "${frame.commandId}_${System.currentTimeMillis()}")
            .put("commandId", frame.commandId)
            .put("orderId", frame.orderId)
            .put("status", frame.status)
            .put("success", frame.success)
            .put("currency", frame.currency)
            .put("billAmount", frame.billAmount)
            .put("tipAmount", frame.tipAmount)
            .put("totalAmount", frame.totalAmount)
            .put("externalTransactionId", frame.externalTransactionId)
            .put("rrn", frame.rrn)
            .put("authCode", frame.authCode)
            .put("terminalId", frame.terminalId)
            .put("receiptText", PcEcrSafeLogSanitizer.sanitize(frame.receipt?.text))
            .put("ecrSendStatus", sendStatus)
            .put("ecrSendError", PcEcrSafeLogSanitizer.sanitize(sendError))
            .put("createdAt", System.currentTimeMillis())
        file.appendText(json.toString() + "\n")
    }
}


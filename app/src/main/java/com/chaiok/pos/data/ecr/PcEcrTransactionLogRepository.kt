package com.chaiok.pos.data.ecr

import android.content.Context
import android.util.Log
import com.chaiok.pos.domain.model.PcEcrOperationType
import java.io.File
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PcEcrTransactionLogRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun save(entry: PcEcrTransactionLogEntry): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = resolveLogFile()
                val json = JSONObject()
                    .put("id", "${entry.commandId ?: "no-command"}_${System.currentTimeMillis()}")
                    .put("commandId", entry.commandId)
                    .put("orderId", entry.orderId)
                    .put("operationType", entry.operationType.name)
                    .put("status", entry.status)
                    .put("resultCode", entry.resultCode)
                    .put("currency", entry.currency)
                    .put("amount", entry.amount?.toPlainString())
                    .put("tipAmount", entry.tipAmount?.toPlainString())
                    .put("rrn", maskRrn(entry.rrn))
                    .put("authCode", maskAuthCode(entry.authCode))
                    .put("terminalId", entry.terminalId)
                    .put("ecrSendStatus", entry.sendStatus)
                    .put("ecrSendError", PcEcrSafeLogSanitizer.sanitize(entry.sendError))
                    .put("createdAt", System.currentTimeMillis())
                file.appendText(json.toString() + "\n")
            }.onFailure { Log.e(TAG, "PC ECR transaction log save failed", it) }
        }

    private fun resolveLogFile(): File {
        val dir = File(appContext.filesDir, LOG_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, LOG_FILE_NAME)
    }

    private fun maskRrn(value: String?): String? = value?.let {
        if (it.length <= 4) "****" else "****${it.takeLast(4)}"
    }

    private fun maskAuthCode(value: String?): String? = value?.let {
        if (it.length <= 2) "**" else "****${it.takeLast(2)}"
    }

    private companion object {
        const val TAG = "PcUsbEcrFlow"
        const val LOG_DIR_NAME = "logs"
        const val LOG_FILE_NAME = "pc_transactions.jsonl"
    }
}

data class PcEcrTransactionLogEntry(
    val commandId: String?,
    val orderId: String?,
    val operationType: PcEcrOperationType,
    val status: String,
    val resultCode: String?,
    val currency: String,
    val amount: BigDecimal?,
    val tipAmount: BigDecimal?,
    val rrn: String?,
    val authCode: String?,
    val terminalId: String?,
    val sendStatus: String,
    val sendError: String?
)

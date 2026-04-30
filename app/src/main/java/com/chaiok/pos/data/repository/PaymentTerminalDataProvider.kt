package com.chaiok.pos.data.repository

import android.util.Log
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.PaymentResult
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.TerminalDataProvider

class PaymentTerminalDataProvider(
    private val paymentApi: PaymentTerminalApi
) : TerminalDataProvider {

    override suspend fun getTerminalInfo(): TerminalInfo {
        Log.e("LoginFlow", "PaymentTerminalDataProvider called")
        val result = paymentApi.getTerminalData()

        val terminalData = when (result) {
            is PaymentTerminalDataResult.Success -> {
                Log.e("LoginFlow", "PaymentTerminalDataResult.Success")
                result.data
            }
            is PaymentTerminalDataResult.Error -> {
                Log.e(
                    "LoginFlow",
                    "PaymentTerminalDataResult.Error type=${result.type} reason=${result.reason}"
                )
                throw when (result.type) {
                    PaymentTerminalDataErrorType.NotReady -> DomainError.TerminalDataNotReady
                    PaymentTerminalDataErrorType.InvalidData -> DomainError.TerminalDataInvalid
                }
            }
        }

        if (terminalData.serialNumber.isBlank() || terminalData.tid.isBlank()) {
            Log.e("LoginFlow", "terminal data invalid: empty serial or tid")
            throw DomainError.TerminalDataInvalid
        }

        Log.e(
            "LoginFlow",
            "PaymentTerminalDataProvider returning serial=***${terminalData.serialNumber.takeLast(4)} tid=***${terminalData.tid.takeLast(4)}"
        )
        return TerminalInfo(
            serialNumber = terminalData.serialNumber,
            tid = terminalData.tid
        )
    }
}

interface PaymentTerminalApi {
    suspend fun getTerminalData(): PaymentTerminalDataResult
    suspend fun pay(amountRub: Double): PaymentResult
}

sealed interface PaymentTerminalDataResult {
    data class Success(val data: PaymentTerminalData) : PaymentTerminalDataResult
    data class Error(
        val reason: String? = null,
        val type: PaymentTerminalDataErrorType = PaymentTerminalDataErrorType.NotReady
    ) : PaymentTerminalDataResult
}

enum class PaymentTerminalDataErrorType {
    NotReady,
    InvalidData
}

data class PaymentTerminalData(
    val serialNumber: String,
    val tid: String
)

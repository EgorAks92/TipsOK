package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.error.TerminalDataInvalidException
import com.chaiok.pos.domain.error.TerminalDataNotReadyException
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.TerminalDataProvider

class PaymentTerminalDataProvider(
    private val paymentApi: PaymentTerminalApi
) : TerminalDataProvider {
    override suspend fun getTerminalInfo(): TerminalInfo {
        val terminalData = when (val result = paymentApi.getTerminalData()) {
            is PaymentTerminalDataResult.Success -> result.data
            is PaymentTerminalDataResult.Error -> throw TerminalDataNotReadyException()
        }
        if (terminalData.serialNumber.isBlank() || terminalData.tid.isBlank()) {
            throw TerminalDataInvalidException()
        }

        return TerminalInfo(
            serialNumber = terminalData.serialNumber,
            tid = terminalData.tid
        )
    }
}

interface PaymentTerminalApi {
    suspend fun getTerminalData(): PaymentTerminalDataResult
}

sealed interface PaymentTerminalDataResult {
    data class Success(val data: PaymentTerminalData) : PaymentTerminalDataResult
    data class Error(val reason: String? = null) : PaymentTerminalDataResult
}

data class PaymentTerminalData(
    val serialNumber: String,
    val tid: String
)

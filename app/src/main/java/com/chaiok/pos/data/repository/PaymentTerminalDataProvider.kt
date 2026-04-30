package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.TerminalDataProvider

class PaymentTerminalDataProvider(
    private val paymentApi: PaymentTerminalApi
) : TerminalDataProvider {

    override suspend fun getTerminalInfo(): TerminalInfo {
        val result = paymentApi.getTerminalData()

        val terminalData = when (result) {
            is PaymentTerminalDataResult.Success -> result.data
            is PaymentTerminalDataResult.Error -> {
                throw when (result.type) {
                    PaymentTerminalDataErrorType.NotReady -> DomainError.TerminalDataNotReady
                    PaymentTerminalDataErrorType.InvalidData -> DomainError.TerminalDataInvalid
                }
            }
        }

        if (terminalData.serialNumber.isBlank() || terminalData.tid.isBlank()) {
            throw DomainError.TerminalDataInvalid
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

package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcPaymentResponse
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import kotlinx.coroutines.flow.Flow

interface PcPaymentCommandRepository {
    fun observeCommands(): Flow<PcPaymentCommand>
    fun observeStatus(): Flow<PcUsbConnectionStatus>
    suspend fun sendResponse(response: PcPaymentResponse): Result<Unit>
}

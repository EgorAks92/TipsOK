package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcPaymentResponse
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import kotlinx.coroutines.flow.Flow

interface PcPaymentCommandRepository {
    fun observeCommands(): Flow<PcPaymentCommand>
    fun observeStatus(): Flow<PcUsbConnectionStatus>
    suspend fun sendResponse(response: PcPaymentResponse): Result<Unit>
    suspend fun sendPaymentResult(frame: ChaiOkEcrPaymentResultFrame): Result<Unit>
    suspend fun sendArcus2PaymentResult(sourceCommand: PcPaymentCommand, result: PcEcrFinalPaymentResult, receiptText: String?, settings: Arcus2NewWaySettings, terminalId: String? = null): Result<Unit>
    suspend fun sendArcus2StatusIfActive(statusText: String, settings: Arcus2NewWaySettings): Result<Unit>
    suspend fun listenOnce()
    suspend fun pauseForPayment(): Result<Unit>
    suspend fun resumeAfterPayment(): Result<Unit>
    suspend fun stopCompletely(): Result<Unit>
    @Deprecated("Use stopCompletely()")
    suspend fun stop(): Result<Unit> = stopCompletely()
}

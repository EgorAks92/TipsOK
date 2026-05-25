package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcPaymentResponse
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import kotlinx.coroutines.flow.Flow

interface PcPaymentCommandRepository {

    fun observeCommands(): Flow<PcPaymentCommand>

    fun observeStatus(): Flow<PcUsbConnectionStatus>

    suspend fun sendResponse(response: PcPaymentResponse): Result<Unit>

    suspend fun sendPaymentResult(frame: ChaiOkEcrPaymentResultFrame): Result<Unit>

    suspend fun listenOnce()

    suspend fun pauseForPayment(): Result<Unit>

    suspend fun resumeAfterPayment(): Result<Unit>

    suspend fun stopCompletely(): Result<Unit>

    @Deprecated("Use stopCompletely()")
    suspend fun stop(): Result<Unit> = stopCompletely()
}

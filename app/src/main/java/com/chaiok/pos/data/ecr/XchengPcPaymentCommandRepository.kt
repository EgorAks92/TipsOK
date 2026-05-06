package com.chaiok.pos.data.ecr

import android.util.Log
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcPaymentResponse
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.repository.PcPaymentCommandRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class XchengPcPaymentCommandRepository(private val client: XchengWireEcrPortClient) : PcPaymentCommandRepository {
    private val commands = MutableSharedFlow<PcPaymentCommand>(extraBufferCapacity = 1)
    private val status = MutableStateFlow<PcUsbConnectionStatus>(PcUsbConnectionStatus.Idle)

    override fun observeCommands(): Flow<PcPaymentCommand> = commands
    override fun observeStatus(): Flow<PcUsbConnectionStatus> = status
    override suspend fun sendResponse(response: PcPaymentResponse): Result<Unit> = Result.success(Unit) // TODO protocol response

    suspend fun listenOnce() {
        status.value = PcUsbConnectionStatus.Binding
        val started = client.start()
        if (started.isFailure) {
            status.value = PcUsbConnectionStatus.Error(started.exceptionOrNull()?.message ?: "bind error")
            client.closeAll()
            delay(500)
            return
        }

        val connected = client.openAndConnect()
        if (connected.isFailure) {
            status.value = PcUsbConnectionStatus.Error(connected.exceptionOrNull()?.message ?: "connect error")
            client.closeAll()
            delay(300)
            return
        }

        status.value = PcUsbConnectionStatus.WaitingForData
        val received = client.receiveOnce()
        if (received.isFailure) {
            status.value = PcUsbConnectionStatus.Error(received.exceptionOrNull()?.message ?: "receive error")
            client.closeAll()
            delay(300)
            return
        }

        val bytes = received.getOrNull()
        Log.i("PcUsbEcrFlow", "recv bytes=${bytes?.size ?: 0}")
        val command = bytes?.let { PcPaymentCommandParser.parse(it) }
        if (command != null) {
            Log.i("PcUsbEcrFlow", "parsed amount=${command.amount} commandId=${command.commandId ?: "-"} orderId=${command.orderId ?: "-"}")
            commands.emit(command)
        }

        client.closeAll()
        status.value = PcUsbConnectionStatus.Idle
        delay(200)
    }

    suspend fun stop() { client.stop(); status.value = PcUsbConnectionStatus.Idle }
}

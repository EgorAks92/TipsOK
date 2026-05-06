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

class XchengPcPaymentCommandRepository(
    private val client: XchengWireEcrPortClient
) : PcPaymentCommandRepository {

    private val commands = MutableSharedFlow<PcPaymentCommand>(
        extraBufferCapacity = 1
    )

    private val status = MutableStateFlow<PcUsbConnectionStatus>(
        PcUsbConnectionStatus.Idle
    )

    override fun observeCommands(): Flow<PcPaymentCommand> = commands

    override fun observeStatus(): Flow<PcUsbConnectionStatus> = status

    override suspend fun sendResponse(response: PcPaymentResponse): Result<Unit> {
        return client.send(response.payload.toByteArray(Charsets.UTF_8))
    }

    suspend fun listenOnce() {
        status.value = PcUsbConnectionStatus.BindingService

        val bound = client.bindService()
        if (bound.isFailure) {
            val message = bound.exceptionOrNull()?.message ?: "bind error"
            status.value = PcUsbConnectionStatus.Error(message)
            client.closeAll()
            delay(ERROR_VISIBLE_DELAY_MS)
            return
        }

        status.value = PcUsbConnectionStatus.ServiceBound
        delay(STEP_VISIBLE_DELAY_MS)

        status.value = PcUsbConnectionStatus.OpeningPort
        delay(STEP_VISIBLE_DELAY_MS)

        status.value = PcUsbConnectionStatus.ConnectingPort

        val connected = client.openAndConnect()
        if (connected.isFailure) {
            val message = connected.exceptionOrNull()?.message ?: "connect error"
            status.value = PcUsbConnectionStatus.Error(message)
            client.closePortOnly()
            delay(ERROR_VISIBLE_DELAY_MS)
            return
        }

        status.value = PcUsbConnectionStatus.Connected
        delay(STEP_VISIBLE_DELAY_MS)

        status.value = PcUsbConnectionStatus.WaitingForData

        val received = client.receiveOnce()
        if (received.isFailure) {
            val message = received.exceptionOrNull()?.message ?: "receive error"
            status.value = PcUsbConnectionStatus.Error(message)
            client.closePortOnly()
            delay(ERROR_VISIBLE_DELAY_MS)
            return
        }

        val bytes: ByteArray? = received.getOrNull()

        Log.i(TAG, "recv bytes=${bytes?.size ?: 0}")

        if (bytes != null && bytes.isNotEmpty()) {
            Log.i(TAG, "recv hex=${bytes.toHexPreview()}")

            val command = PcPaymentCommandParser.parse(bytes)

            if (command != null) {
                Log.i(
                    TAG,
                    "parsed amount=${command.amount} " +
                            "commandId=${command.commandId ?: "-"} " +
                            "orderId=${command.orderId ?: "-"}"
                )

                commands.emit(command)
            } else {
                Log.w(
                    TAG,
                    "payload received but parser returned null. " +
                            "hex=${bytes.toHexPreview()}"
                )
            }
        }

        client.closePortOnly()
        status.value = PcUsbConnectionStatus.Idle

        delay(LISTEN_LOOP_DELAY_MS)
    }

    suspend fun stop() {
        client.stop()
        status.value = PcUsbConnectionStatus.Idle
    }

    private fun ByteArray.toHexPreview(limit: Int = 64): String {
        return take(limit).joinToString(" ") { byte ->
            "%02X".format(byte)
        }
    }

    private companion object {
        private const val TAG = "PcUsbEcrFlow"

        private const val STEP_VISIBLE_DELAY_MS = 100L
        private const val LISTEN_LOOP_DELAY_MS = 300L
        private const val ERROR_VISIBLE_DELAY_MS = 2500L
    }
}
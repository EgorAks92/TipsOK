package com.chaiok.pos.presentation.pc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.data.ecr.XchengPcPaymentCommandRepository
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PcCommandIdleUiState(
    val status: String = "Ожидание запуска"
)

sealed interface PcCommandIdleEvent {
    data class OpenTipSelection(
        val amount: BigDecimal,
        val commandId: String?,
        val orderId: String?
    ) : PcCommandIdleEvent
}

class PcCommandIdleViewModel(
    private val repo: XchengPcPaymentCommandRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PcCommandIdleUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<PcCommandIdleEvent>()
    val events = _events.asSharedFlow()

    private val listeningEnabled = MutableStateFlow(false)

    private val seenCommandIds = mutableSetOf<String>()

    private var lastNoIdSignature: String? = null
    private var lastNoIdAtMs: Long = 0L

    private val cleanupJob: Job = SupervisorJob()
    private val cleanupScope = CoroutineScope(cleanupJob + Dispatchers.IO)

    init {
        observeUsbStatus()
        runListeningLoop()
        observeCommands()
    }

    fun resumeListening() {
        listeningEnabled.value = true
    }

    fun pauseListening() {
        listeningEnabled.value = false
    }

    override fun onCleared() {
        cleanupScope.launch {
            repo.stop()
            cleanupJob.cancel()
        }

        super.onCleared()
    }

    private fun observeUsbStatus() {
        viewModelScope.launch {
            repo.observeStatus().collect { status ->
                _state.value = PcCommandIdleUiState(
                    status = statusText(status)
                )
            }
        }
    }

    private fun runListeningLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (listeningEnabled.value) {
                    repo.listenOnce()
                } else {
                    delay(PAUSED_LOOP_DELAY_MS)
                }
            }
        }
    }

    private fun observeCommands() {
        viewModelScope.launch {
            repo.observeCommands().collect { command ->
                val commandId = command.commandId

                if (commandId != null && !seenCommandIds.add(commandId)) {
                    return@collect
                }

                if (commandId == null) {
                    val signature = "${command.amount}|${command.rawPayloadPreview.orEmpty()}"
                    val now = System.currentTimeMillis()

                    if (
                        signature == lastNoIdSignature &&
                        now - lastNoIdAtMs <= DUPLICATE_NO_ID_WINDOW_MS
                    ) {
                        return@collect
                    }

                    lastNoIdSignature = signature
                    lastNoIdAtMs = now
                }

                listeningEnabled.value = false

                _events.emit(
                    PcCommandIdleEvent.OpenTipSelection(
                        amount = command.amount,
                        commandId = command.commandId,
                        orderId = command.orderId
                    )
                )
            }
        }
    }

    private fun statusText(status: PcUsbConnectionStatus): String {
        return when (status) {
            PcUsbConnectionStatus.Idle ->
                "Ожидание запуска"

            PcUsbConnectionStatus.BindingService ->
                "Подключение к сервису USB-кассы"

            PcUsbConnectionStatus.ServiceBound ->
                "Сервис USB-кассы подключён"

            PcUsbConnectionStatus.OpeningPort ->
                "Открытие USB-порта"

            PcUsbConnectionStatus.ConnectingPort ->
                "Подключение к USB-порту /dev/ttyACM0"

            PcUsbConnectionStatus.Connected ->
                "USB-порт подключён"

            PcUsbConnectionStatus.WaitingForData ->
                "Ожидание команды оплаты от кассы"

            is PcUsbConnectionStatus.Error ->
                "Ошибка USB: ${status.message}"
        }
    }

    private companion object {
        private const val PAUSED_LOOP_DELAY_MS = 150L
        private const val DUPLICATE_NO_ID_WINDOW_MS = 5_000L
    }
}
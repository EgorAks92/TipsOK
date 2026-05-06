package com.chaiok.pos.presentation.pc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.data.ecr.XchengPcPaymentCommandRepository
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PcCommandIdleUiState(val status: String = "Idle")
sealed interface PcCommandIdleEvent {
    data class OpenTipSelection(val amount: BigDecimal, val commandId: String?, val orderId: String?) : PcCommandIdleEvent
}

class PcCommandIdleViewModel(private val repo: XchengPcPaymentCommandRepository) : ViewModel() {
    private val _state = MutableStateFlow(PcCommandIdleUiState())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<PcCommandIdleEvent>()
    val events = _events.asSharedFlow()

    private val seenCommandIds = mutableSetOf<String>()
    private val listeningEnabled = MutableStateFlow(false)
    private var lastNoIdSignature: String? = null
    private var lastNoIdAtMs: Long = 0L
    private val cleanupJob: Job = SupervisorJob()
    private val cleanupScope = CoroutineScope(cleanupJob + Dispatchers.IO)

    init {
        viewModelScope.launch { repo.observeStatus().collect { _state.value = PcCommandIdleUiState(statusText(it)) } }
        viewModelScope.launch {
            while (isActive) {
                if (listeningEnabled.value) {
                    repo.listenOnce()
                } else {
                    delay(150)
                }
            }
        }
        viewModelScope.launch {
            repo.observeCommands().collect { cmd ->
                val id = cmd.commandId
                if (id != null && !seenCommandIds.add(id)) return@collect
                if (id == null) {
                    val signature = "${cmd.amount}|${cmd.rawPayloadPreview.orEmpty()}"
                    val now = System.currentTimeMillis()
                    if (signature == lastNoIdSignature && now - lastNoIdAtMs <= 5_000L) return@collect
                    lastNoIdSignature = signature
                    lastNoIdAtMs = now
                }
                listeningEnabled.value = false
                _events.emit(PcCommandIdleEvent.OpenTipSelection(cmd.amount, cmd.commandId, cmd.orderId))
            }
        }
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

    private fun statusText(status: PcUsbConnectionStatus): String = when (status) {
        PcUsbConnectionStatus.Binding -> "Подключение к USB сервису"
        PcUsbConnectionStatus.Connected -> "Сервис подключен"
        PcUsbConnectionStatus.WaitingForData -> "Ожидание команды"
        PcUsbConnectionStatus.Idle -> "Ожидание старта"
        is PcUsbConnectionStatus.Error -> "Ошибка: ${status.message}"
    }
}

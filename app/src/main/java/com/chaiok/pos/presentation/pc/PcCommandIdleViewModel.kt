package com.chaiok.pos.presentation.pc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.repository.PcPaymentCommandRepository
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PcCommandIdleViewModel(
    private val repository: PcPaymentCommandRepository
) : ViewModel() {

    private val listeningEnabled = MutableStateFlow(false)

    private val _status = MutableStateFlow<PcUsbConnectionStatus>(
        PcUsbConnectionStatus.Idle
    )

    val status: StateFlow<PcUsbConnectionStatus> = _status.asStateFlow()
    val state: StateFlow<PcUsbConnectionStatus> = status
    val uiState: StateFlow<PcUsbConnectionStatus> = status

    private val _events = MutableSharedFlow<PcCommandIdleEvent>(
        extraBufferCapacity = 1
    )

    val events: SharedFlow<PcCommandIdleEvent> = _events.asSharedFlow()

    private val cleanupScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var lastCommandId: String? = null
    private var lastNoIdFingerprint: String? = null
    private var lastNoIdAtMs: Long = 0L

    init {
        observeStatus()
        observeCommands()
        startListeningLoop()
    }

    fun resumeListening() {
        resetDuplicateGuard()

        if (!listeningEnabled.value) {
            listeningEnabled.value = true
        }
    }

    fun pauseListening() {
        if (listeningEnabled.value) {
            listeningEnabled.value = false
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.stop()
        }
    }

    private fun observeStatus() {
        viewModelScope.launch {
            repository.observeStatus().collect(
                object : FlowCollector<PcUsbConnectionStatus> {
                    override suspend fun emit(value: PcUsbConnectionStatus) {
                        _status.value = value
                    }
                }
            )
        }
    }

    private fun observeCommands() {
        viewModelScope.launch {
            repository.observeCommands().collect(
                object : FlowCollector<PcPaymentCommand> {
                    override suspend fun emit(value: PcPaymentCommand) {
                        handleCommand(value)
                    }
                }
            )
        }
    }

    private fun startListeningLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            listeningEnabled.collectLatest { enabled: Boolean ->
                if (!enabled) {
                    return@collectLatest
                }

                while (isActive && listeningEnabled.value) {
                    repository.listenOnce()

                    if (listeningEnabled.value) {
                        delay(LISTEN_LOOP_DELAY_MS)
                    }
                }
            }
        }
    }

    private suspend fun handleCommand(command: PcPaymentCommand) {
        if (shouldIgnoreDuplicate(command)) {
            return
        }

        listeningEnabled.value = false

        withContext(Dispatchers.IO) {
            repository.stop()
        }

        delay(POS_SERVICE_RELEASE_DELAY_MS)

        _events.emit(
            PcCommandIdleEvent.OpenTipSelection(
                amount = command.amount,
                commandId = command.commandId,
                orderId = command.orderId
            )
        )
    }

    private fun shouldIgnoreDuplicate(command: PcPaymentCommand): Boolean {
        val commandId = command.commandId

        if (!commandId.isNullOrBlank()) {
            if (commandId == lastCommandId) {
                return true
            }

            lastCommandId = commandId
            return false
        }

        val now = System.currentTimeMillis()
        val fingerprint = buildString {
            append(command.amount.stripTrailingZeros().toPlainString())
            append("|")
            append(command.rawPayloadPreview.orEmpty())
        }

        val duplicate = fingerprint == lastNoIdFingerprint &&
                now - lastNoIdAtMs < NO_ID_DEDUPE_WINDOW_MS

        lastNoIdFingerprint = fingerprint
        lastNoIdAtMs = now

        return duplicate
    }

    private fun resetDuplicateGuard() {
        lastCommandId = null
        lastNoIdFingerprint = null
        lastNoIdAtMs = 0L
    }

    override fun onCleared() {
        cleanupScope.launch {
            repository.stop()
            cleanupScope.cancel()
        }

        super.onCleared()
    }

    private companion object {
        private const val LISTEN_LOOP_DELAY_MS = 300L
        private const val NO_ID_DEDUPE_WINDOW_MS = 5_000L
        private const val POS_SERVICE_RELEASE_DELAY_MS = 500L
    }
}

sealed interface PcCommandIdleEvent {
    data class OpenTipSelection(
        val amount: BigDecimal,
        val commandId: String?,
        val orderId: String?
    ) : PcCommandIdleEvent
}
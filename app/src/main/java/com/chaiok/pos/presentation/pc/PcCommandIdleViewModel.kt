package com.chaiok.pos.presentation.pc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PcEcrOperationType
import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.domain.model.PostPaymentFeedbackPayload
import com.chaiok.pos.domain.repository.PcPaymentCommandRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.usecase.LoginWithPinUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.SubmitPostPaymentFeedbackUseCase
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val UNLOCK_PIN_MAX_LENGTH = 4
private const val TAG = "PcCommandIdle"

data class PostPaymentFeedbackUiState(
    val visible: Boolean = false,
    val commandId: String? = null,
    val transactionId: String? = null,
    val serviceRating: Int? = null,
    val kitchenRating: Int? = null,
    val secondsLeft: Int = 15,
    val submitting: Boolean = false
)

data class PcCommandIdleUiState(
    val connectionStatus: PcUsbConnectionStatus = PcUsbConnectionStatus.Idle,
    val images: List<String> = emptyList(),
    val showUnlockDialog: Boolean = false,
    val unlockPin: String = "",
    val isUnlocking: Boolean = false,
    val unlockError: String? = null,
    val unlockPinMaxLength: Int = UNLOCK_PIN_MAX_LENGTH,
    val statusMessage: String? = null,
    val designStyle: PcCompactPaymentDesignStyle = PcCompactPaymentDesignStyle.DEFAULT,
    val postPaymentFeedback: PostPaymentFeedbackUiState = PostPaymentFeedbackUiState()
)

private data class UnlockState(
    val showDialog: Boolean = false,
    val pin: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class PcCommandIdleViewModel(
    private val repository: PcPaymentCommandRepository,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val loginWithPinUseCase: LoginWithPinUseCase,
    private val sessionRepository: SessionRepository,
    private val submitPostPaymentFeedbackUseCase: SubmitPostPaymentFeedbackUseCase
) : ViewModel() {

    private val listeningEnabled = MutableStateFlow(false)

    private val _status = MutableStateFlow<PcUsbConnectionStatus>(
        PcUsbConnectionStatus.Idle
    )

    private val _uiState = MutableStateFlow(PcCommandIdleUiState())
    val uiState: StateFlow<PcCommandIdleUiState> = _uiState.asStateFlow()

    private val unlockState = MutableStateFlow(UnlockState())

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
    private var resumeListeningJob: Job? = null
    private val pcStatusMessage = MutableStateFlow<String?>(null)
    private val feedbackState = MutableStateFlow(PostPaymentFeedbackUiState())
    private val feedbackPayload = MutableStateFlow<PostPaymentFeedbackPayload?>(null)
    private val submittedFeedbackCommandIds = mutableSetOf<String>()
    private val feedbackSubmitMutex = Mutex()

    init {
        observeStatus()
        observeUiState()
        observeCommands()
        startListeningLoop()
    }

    fun resumeListening() {
        resetDuplicateGuard()

        if (!listeningEnabled.value) {
            listeningEnabled.value = true
        }

        if (resumeListeningJob?.isActive == true) {
            Log.i(TAG, "PC idle resume ECR skipped: resume already in progress")
            return
        }

        resumeListeningJob = viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "PC idle resume ECR listening")
            repository.resumeAfterPayment()
                .onFailure { Log.e(TAG, "PC idle resume ECR failed", it) }
        }
    }

    fun pauseListening() {
        if (listeningEnabled.value) {
            listeningEnabled.value = false
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.pauseForPayment()
                .onFailure { Log.w(TAG, "PC idle pause ECR failed", it) }
        }
    }

    fun stopListeningCompletely() {
        if (listeningEnabled.value) {
            listeningEnabled.value = false
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.stopCompletely()
                .onFailure { Log.e(TAG, "PC idle full ECR stop failed", it) }
        }
    }


    fun showPostPaymentFeedback(payload: PostPaymentFeedbackPayload) {
        val commandId = payload.commandId.ifBlank { return }
        if (submittedFeedbackCommandIds.contains(commandId)) {
            return
        }

        feedbackPayload.value = payload
        feedbackState.value = PostPaymentFeedbackUiState(
            visible = true,
            commandId = commandId,
            transactionId = payload.transactionId,
            secondsLeft = FEEDBACK_TIMEOUT_SECONDS
        )
        Log.i(TAG, "POST_PAYMENT_FEEDBACK shown commandId=$commandId")
    }

    fun onFeedbackServiceRatingSelected(value: Int) {
        val rating = value.coerceIn(1, 5)
        Log.i(TAG, "POST_PAYMENT_FEEDBACK service rating selected value=$rating")
        feedbackState.update { current ->
            if (!current.visible || current.submitting) current else current.copy(serviceRating = rating)
        }
        submitFeedbackIfBothSelected()
    }

    fun onFeedbackKitchenRatingSelected(value: Int) {
        val rating = value.coerceIn(1, 5)
        Log.i(TAG, "POST_PAYMENT_FEEDBACK kitchen rating selected value=$rating")
        feedbackState.update { current ->
            if (!current.visible || current.submitting) current else current.copy(kitchenRating = rating)
        }
        submitFeedbackIfBothSelected()
    }

    fun onFeedbackTimerTick() {
        val current = feedbackState.value
        if (!current.visible || current.submitting || current.secondsLeft <= 0) return
        val next = (current.secondsLeft - 1).coerceAtLeast(0)
        feedbackState.value = current.copy(secondsLeft = next)
        if (next == 0) {
            viewModelScope.launch { finishFeedback(reason = "timeout", submitIfAnyRating = true) }
        }
    }

    fun closeFeedbackByUser() {
        Log.i(TAG, "POST_PAYMENT_FEEDBACK closed reason=user_close")
        viewModelScope.launch { finishFeedback(reason = "user_close", submitIfAnyRating = true) }
    }

    private fun submitFeedbackIfBothSelected() {
        val current = feedbackState.value
        if (current.serviceRating != null && current.kitchenRating != null) {
            Log.i(TAG, "POST_PAYMENT_FEEDBACK auto submit reason=both_selected")
            viewModelScope.launch { finishFeedback(reason = "both_selected", submitIfAnyRating = true) }
        }
    }

    private suspend fun finishFeedback(reason: String, submitIfAnyRating: Boolean) {
        feedbackSubmitMutex.withLock {
            val current = feedbackState.value
            if (!current.visible || current.submitting) return@withLock

            val commandId = current.commandId.orEmpty()
            val hasAnyRating = current.serviceRating != null || current.kitchenRating != null
            if (reason == "timeout") {
                Log.i(TAG, "POST_PAYMENT_FEEDBACK auto submit reason=timeout")
            }

            if (!submitIfAnyRating || !hasAnyRating || submittedFeedbackCommandIds.contains(commandId)) {
                feedbackState.value = PostPaymentFeedbackUiState()
                feedbackPayload.value = null
                return@withLock
            }

            feedbackState.value = current.copy(submitting = true)
            val payload = feedbackPayload.value?.copy(
                serviceRating = current.serviceRating,
                kitchenRating = current.kitchenRating
            )

            if (payload == null || commandId.isBlank()) {
                feedbackState.value = PostPaymentFeedbackUiState()
                feedbackPayload.value = null
                return@withLock
            }

            submittedFeedbackCommandIds += commandId
            submitPostPaymentFeedbackUseCase(payload)
                .onSuccess { Log.i(TAG, "POST_PAYMENT_FEEDBACK submit success") }
                .onFailure { Log.w(TAG, "POST_PAYMENT_FEEDBACK submit failed reason=${safeFeedbackError(it)}") }

            feedbackState.value = PostPaymentFeedbackUiState()
            feedbackPayload.value = null
        }
    }

    private fun dismissFeedbackForNewCommandIfVisible() {
        if (!feedbackState.value.visible) return
        Log.i(TAG, "POST_PAYMENT_FEEDBACK dismissed because new ECR command received")
        feedbackState.value = PostPaymentFeedbackUiState()
        feedbackPayload.value = null
    }

    private fun safeFeedbackError(error: Throwable): String = error::class.simpleName ?: "feedback_failed"

    fun openUnlockDialog() {
        unlockState.value = UnlockState(showDialog = true)
    }

    fun closeUnlockDialog() {
        val current = unlockState.value
        if (current.isLoading) return
        unlockState.value = UnlockState()
    }

    fun onUnlockDigit(digit: String) {
        val current = unlockState.value
        if (
            !current.showDialog ||
            current.isLoading ||
            current.pin.length >= UNLOCK_PIN_MAX_LENGTH ||
            digit.length != 1 ||
            !digit[0].isDigit()
        ) return
        unlockState.value = current.copy(pin = current.pin + digit, error = null)
    }

    fun onUnlockBackspace() {
        val current = unlockState.value
        if (!current.showDialog || current.isLoading || current.pin.isEmpty()) return
        unlockState.value = current.copy(pin = current.pin.dropLast(1), error = null)
    }

    fun submitUnlockPin() {
        val current = unlockState.value
        if (!current.showDialog || current.isLoading) return
        if (current.pin.isBlank()) {
            unlockState.value = current.copy(error = "Введите PIN")
            return
        }

        val pin = current.pin
        viewModelScope.launch {
            unlockState.value = unlockState.value.copy(isLoading = true, error = null)
            val result = loginWithPinUseCase(pin)
            result.onSuccess {
                unlockState.value = UnlockState()
                _events.emit(PcCommandIdleEvent.NavigateToSettings)
            }.onFailure {
                unlockState.value = unlockState.value.copy(
                    pin = "",
                    isLoading = false,
                    error = "Неверный пароль"
                )
            }
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

    private fun observeUiState() {
        viewModelScope.launch {
            combine(
                _status,
                observeSettingsUseCase(),
                unlockState,
                pcStatusMessage,
                combine(
                    feedbackState,
                    combine(sessionRepository.profileId, sessionRepository.accessToken) { profileId, token ->
                        profileId != null && !token.isNullOrBlank()
                    }
                ) { feedback, waiterAuthorized -> feedback to waiterAuthorized }
            ) { status, settings, unlock, pcMessage, feedbackAndAuth ->
                val (feedback, waiterAuthorized) = feedbackAndAuth
                val configuredImages = settings.pcIdleImages.filter { it.isNotBlank() }
                PcCommandIdleUiState(
                    connectionStatus = status,
                    images = if (configuredImages.isNotEmpty()) configuredImages else listOf(DEFAULT_IMAGE),
                    showUnlockDialog = unlock.showDialog,
                    unlockPin = unlock.pin,
                    isUnlocking = unlock.isLoading,
                    unlockError = unlock.error,
                    unlockPinMaxLength = UNLOCK_PIN_MAX_LENGTH,
                    statusMessage = pcMessage ?: if (waiterAuthorized) null else "Ожидание авторизации официанта",
                    designStyle = settings.pcCompactPaymentDesignStyle,
                    postPaymentFeedback = feedback
                )
            }.collect { nextState ->
                _uiState.value = nextState
            }
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

                    if (repository.lastListenConsumedIdleStandaloneControl) {
                        Log.i(TAG, "ARCUS2 idle standalone control consumed; continue listening immediately")
                        continue
                    }

                    if (listeningEnabled.value) {
                        delay(LISTEN_LOOP_DELAY_MS)
                    }
                }
            }
        }
    }

    private suspend fun handleCommand(command: PcPaymentCommand) {
        dismissFeedbackForNewCommandIfVisible()

        if (shouldIgnoreDuplicate(command)) {
            return
        }

        listeningEnabled.value = false

        if (command.operationType == PcEcrOperationType.WAITER_LOGIN) {
            handleWaiterLogin(command)
            return
        }

        _events.emit(
            PcCommandIdleEvent.OpenTipSelection(
                amount = command.amount,
                commandId = command.commandId,
                orderId = command.orderId,
                currency = command.currency,
                sourceProtocol = command.sourceProtocol,
                operationType = command.operationType,
                rrn = command.rrn
            )
        )
    }


    private suspend fun handleWaiterLogin(command: PcPaymentCommand) {
        val pin = command.waiterPin
        Log.i(TAG, "WAITER_LOGIN started waiterPinPresent=${!pin.isNullOrBlank()} waiterPinMasked=****")
        pcStatusMessage.value = "Авторизация официанта"
        val result = if (pin.isNullOrBlank()) {
            Result.failure(IllegalArgumentException("missing pin"))
        } else {
            Log.i(TAG, "WAITER_LOGIN terminalLogin start")
            loginWithPinUseCase(pin)
        }

        val finalResult = result.fold(
            onSuccess = { profile ->
                val profileId = sessionRepository.profileId.first()
                val waiterId = sessionRepository.activeWaiterId.first()
                Log.i(
                    TAG,
                    "WAITER_LOGIN terminalLogin success profileId=${profileId ?: "-"} waiterId=${waiterId ?: profile.id} nicknamePresent=${profile.firstName.isNotBlank() || profile.lastName.isNotBlank()}"
                )
                pcStatusMessage.value = "Официант авторизован"
                PcEcrFinalPaymentResult.Approved(resultCode = "00")
            },
            onFailure = { error ->
                Log.w(TAG, "WAITER_LOGIN terminalLogin failed reason=${safeLoginError(error)}")
                pcStatusMessage.value = "Ошибка авторизации официанта"
                PcEcrFinalPaymentResult.Error(message = "waiter login failed")
            }
        )

        Log.i(TAG, "WAITER_LOGIN pin cleared from memory")
        val settings = observeSettingsUseCase().first()
        repository.sendArcus2PaymentResult(
            sourceCommand = command.copy(waiterPin = null),
            result = finalResult,
            receiptText = null,
            settings = settings.arcus2NewWaySettings,
            terminalId = null,
            tipAmount = null
        ).onFailure { Log.e(TAG, "WAITER_LOGIN final result send failed", it) }

        delay(WAITER_LOGIN_STATUS_VISIBLE_MS)
        pcStatusMessage.value = null
        resumeListening()
    }

    private fun safeLoginError(error: Throwable): String = error::class.simpleName ?: "login_failed"
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
            repository.pauseForPayment()
            cleanupScope.cancel()
        }

        super.onCleared()
    }

    private companion object {
        private const val LISTEN_LOOP_DELAY_MS = 300L
        private const val NO_ID_DEDUPE_WINDOW_MS = 5_000L
        private const val DEFAULT_IMAGE = "default"
        private const val WAITER_LOGIN_STATUS_VISIBLE_MS = 1_200L
        private const val FEEDBACK_TIMEOUT_SECONDS = 15
    }
}

sealed interface PcCommandIdleEvent {
    data object NavigateToSettings : PcCommandIdleEvent

    data class OpenTipSelection(
        val amount: BigDecimal,
        val commandId: String?,
        val orderId: String?,
        val currency: String,
        val sourceProtocol: PcEcrProtocol,
        val operationType: PcEcrOperationType,
        val rrn: String?
    ) : PcCommandIdleEvent
}

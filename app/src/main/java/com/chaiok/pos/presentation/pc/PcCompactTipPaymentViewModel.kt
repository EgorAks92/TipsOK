package com.chaiok.pos.presentation.pc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.domain.model.PcEcrOperationType
import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.model.PcPaymentCommand
import com.chaiok.pos.domain.model.PosPaymentCancelPreviousRequest
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.data.ecr.PcEcrPaymentResultMapper
import com.chaiok.pos.data.ecr.PcPaymentTransactionLogRepository
import com.chaiok.pos.domain.repository.PcPaymentCommandRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.usecase.CancelPosPaymentUseCase
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.StartPosPaymentCancelPreviousUseCase
import com.chaiok.pos.domain.usecase.StartPosPaymentUseCase
import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.roundToInt

data class PcCompactTipPaymentUiState(
    val billAmount: Double,
    val availablePercents: List<Double> = emptyList(),
    val selectedPercentIndex: Int = 0,
    val customTipAmount: Double? = null,
    val isCustomTipSelected: Boolean = false,
    val isNoTipsSelected: Boolean = false,
    val showCustomTipButton: Boolean = false,
    val tipConfigLoaded: Boolean = false,
    val serviceFeePercent: Double = 0.0,
    val showServiceFeeToggle: Boolean = true,
    val isServiceFeeEnabled: Boolean = false,
    val paymentStage: CardPresentingStage = CardPresentingStage.Idle,
    val isRestartingPayment: Boolean = false,
    val errorMessage: String? = null,
    val canCancel: Boolean = true,
    val operationType: PcEcrOperationType = PcEcrOperationType.SALE,
    val operationTitle: String = "Оплата",
    val designStyle: PcCompactPaymentDesignStyle = PcCompactPaymentDesignStyle.DEFAULT,
    val visualSettingsLoaded: Boolean = false,
    val currency: String = "RUB"
) {
    fun calculateTipByPercent(percent: Double): Double =
        roundMoney(billAmount * percent / 100.0)

    val selectedTipAmount: Double
        get() = when {
            operationType == PcEcrOperationType.CANCEL_PREVIOUS -> 0.0
            isNoTipsSelected -> 0.0
            isCustomTipSelected -> roundMoney(customTipAmount ?: 0.0)
            else -> availablePercents
                .getOrNull(selectedPercentIndex)
                ?.let(::calculateTipByPercent)
                ?: 0.0
        }

    val serviceFeeAmount: Double
        get() = if (operationType == PcEcrOperationType.CANCEL_PREVIOUS) 0.0
        else if (showServiceFeeToggle && isServiceFeeEnabled && serviceFeePercent > 0.0) roundMoney(selectedTipAmount * serviceFeePercent / 100.0) else 0.0

    val totalAmount: Double
        get() = if (operationType == PcEcrOperationType.CANCEL_PREVIOUS) billAmount else billAmount + selectedTipAmount + serviceFeeAmount

    val amountText: String
        get() = formatRubles(totalAmount)

    val canChangeTips: Boolean
        get() = operationType != PcEcrOperationType.CANCEL_PREVIOUS &&
                !isRestartingPayment && paymentStage in setOf(
            CardPresentingStage.Idle,
            CardPresentingStage.Preparing,
            CardPresentingStage.WaitingForCard
        )

    private fun roundMoney(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}

sealed interface PcCompactTipPaymentEvent {
    data object Approved : PcCompactTipPaymentEvent
    data object CancelledByUser : PcCompactTipPaymentEvent
    data object DeclinedTimeout : PcCompactTipPaymentEvent
}

class PcCompactTipPaymentViewModel(
    private val billAmount: Double,
    private val startPosPaymentUseCase: StartPosPaymentUseCase,
    private val startPosPaymentCancelPreviousUseCase: StartPosPaymentCancelPreviousUseCase,
    private val cancelPosPaymentUseCase: CancelPosPaymentUseCase,
    private val getTransactionRangeUseCase: GetTransactionRangeUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val sessionRepository: SessionRepository,
    private val pcPaymentCommandRepository: PcPaymentCommandRepository,
    private val paymentResultMapper: PcEcrPaymentResultMapper,
    private val transactionLogRepository: PcPaymentTransactionLogRepository,
    private val sourceCommandId: String?,
    private val sourceOrderId: String?,
    private val sourceCurrency: String?,
    private val sourceProtocol: String?,
    private val sourceOperationType: String?,
    private val sourceRrn: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(PcCompactTipPaymentUiState(billAmount = billAmount))
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<PcCompactTipPaymentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val operationMutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var paymentJob: Job? = null
    private var tipSelectionDebounceJob: Job? = null
    private var declinedTimeoutJob: Job? = null
    private var pendingSelectedIndex: Int? = null
    private var activeSelectedTip: PcCompactSelectedTip = PcCompactSelectedTip.Percent(0)

    private var generation = 0L
    private var internalRestartInProgress = false
    private var userCancelInProgress = false
    private var cancelEventSent = false
    private var ignoreNextCancelledFromRestart = false
    private var pcEcrFinalResultSending = false
    private var pcEcrFinalResultSent = false
    private var pendingFinalPcEcrResult: PcEcrFinalPaymentResult? = null
    private var arcus2StatusKeepAliveJob: Job? = null
    private var lastArcus2StatusText: String? = null
    private var commandCurrency: String = "RUB"

    private var waiterId: String = ""
    private var activePaymentServiceFeeEnabled: Boolean = false
    private var terminalId: String = ""
    private val operationType: PcEcrOperationType =
        runCatching {
            PcEcrOperationType.valueOf(sourceOperationType?.ifBlank { "SALE" } ?: "SALE")
        }.getOrDefault(PcEcrOperationType.SALE)
    private val sourceRrnNormalized: String? = sourceRrn?.trim()?.ifBlank { null }

    init {
        viewModelScope.launch { initStateAndStart() }
    }

    private suspend fun initStateAndStart() {
        terminalId = sessionRepository.terminalId.first().orEmpty()
        commandCurrency = sourceCurrency?.ifBlank { null } ?: "RUB"

        val settings = observeSettingsUseCase().first()

        if (isCancelPreviousOperation()) {
            initCancelPreviousStateAndStart(settings)
        } else {
            initSaleStateAndStart(settings)
        }
    }

    private suspend fun initCancelPreviousStateAndStart(settings: AppSettings) {
        _uiState.update {
            it.copy(
                availablePercents = emptyList(),
                selectedPercentIndex = 0,
                customTipAmount = null,
                isCustomTipSelected = false,
                isNoTipsSelected = true,
                showCustomTipButton = false,
                tipConfigLoaded = true,
                serviceFeePercent = 0.0,
                showServiceFeeToggle = false,
                isServiceFeeEnabled = false,
                paymentStage = CardPresentingStage.Preparing,
                isRestartingPayment = false,
                errorMessage = null,
                canCancel = true,
                operationType = PcEcrOperationType.CANCEL_PREVIOUS,
                operationTitle = "Отмена",
                designStyle = settings.pcCompactPaymentDesignStyle,
                visualSettingsLoaded = true,
                currency = commandCurrency
            )
        }
        activeSelectedTip = PcCompactSelectedTip.NoTips
        activePaymentServiceFeeEnabled = false
        observeSettings()

        Log.i(
            TAG,
            "CANCEL_PREVIOUS init start terminalIdBlank=${terminalId.isBlank()} " +
                "rrnPresent=${!sourceRrnNormalized.isNullOrBlank()} amount=$billAmount currency=$commandCurrency"
        )

        pausePcEcrForPayment()
        if (isArcus2Source() && settings.arcus2NewWaySettings.cancelStatusKeepAliveEnabled) {
            startArcus2StatusKeepAlive()
        }

        generation += 1
        startCancelPreviousPayment("initial", generation)
    }

    private suspend fun initSaleStateAndStart(settings: AppSettings) {

        _uiState.update {
            it.copy(
                showServiceFeeToggle = settings.pcCompactServiceFeeEnabled,
                showCustomTipButton = settings.showCustomTipButton,
                designStyle = settings.pcCompactPaymentDesignStyle,
                visualSettingsLoaded = true,
                currency = commandCurrency,
                operationType = operationType,
                operationTitle = "Оплата",
                isServiceFeeEnabled = false
            )
        }

        val profile = observeProfileUseCase().filterNotNull().first()
        waiterId = profile.id

        val range = getTransactionRangeUseCase.observe().first()
        val percents = range?.percents?.takeIf { it.isNotEmpty() } ?: listOf(5.0, 10.0, 15.0)
        val selected = resolveDefaultIndex(percents, range?.defaultIndex)

        _uiState.update {
            it.copy(
                availablePercents = percents,
                selectedPercentIndex = selected,
                serviceFeePercent = profile.serviceFeePercent.coerceAtLeast(0.0),
                showServiceFeeToggle = settings.pcCompactServiceFeeEnabled,
                showCustomTipButton = settings.showCustomTipButton,
                tipConfigLoaded = true,
                designStyle = settings.pcCompactPaymentDesignStyle,
                visualSettingsLoaded = true,
                isServiceFeeEnabled = false,
                paymentStage = CardPresentingStage.Preparing,
                currency = commandCurrency
            )
        }
        observeSettings()

        pausePcEcrForPayment()
        if (isArcus2Source()) {
            startArcus2StatusKeepAlive()
        }

        generation += 1
        val started = startPaymentCurrentAmount("initial", generation)
        if (started) {
            activePaymentServiceFeeEnabled = _uiState.value.isServiceFeeEnabled
            activeSelectedTip = _uiState.value.currentSelectedTip()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                if (isCancelPreviousOperation()) {
                    _uiState.update {
                        it.copy(
                            designStyle = settings.pcCompactPaymentDesignStyle,
                            visualSettingsLoaded = true,
                            showCustomTipButton = false,
                            showServiceFeeToggle = false,
                            isServiceFeeEnabled = false
                        )
                    }
                    return@collect
                }

                var shouldRestartAfterCustomDisabled = false
                var fallbackSelectedTip: PcCompactSelectedTip? = null

                _uiState.update { current ->
                    val disablingSelectedCustom =
                        !settings.showCustomTipButton && current.isCustomTipSelected

                    if (disablingSelectedCustom) {
                        val hasPercents = current.availablePercents.isNotEmpty()
                        shouldRestartAfterCustomDisabled = true
                        fallbackSelectedTip = if (hasPercents) {
                            PcCompactSelectedTip.Percent(0)
                        } else {
                            PcCompactSelectedTip.NoTips
                        }

                        current.copy(
                            showServiceFeeToggle = settings.pcCompactServiceFeeEnabled,
                            showCustomTipButton = false,
                            tipConfigLoaded = true,
                            designStyle = settings.pcCompactPaymentDesignStyle,
                            visualSettingsLoaded = true,
                            isCustomTipSelected = false,
                            isNoTipsSelected = !hasPercents,
                            selectedPercentIndex = if (hasPercents) 0 else current.selectedPercentIndex,
                            customTipAmount = null,
                            errorMessage = null
                        )
                    } else {
                        current.copy(
                            showServiceFeeToggle = settings.pcCompactServiceFeeEnabled,
                            showCustomTipButton = settings.showCustomTipButton,
                            tipConfigLoaded = true,
                            designStyle = settings.pcCompactPaymentDesignStyle,
                            visualSettingsLoaded = true
                        )
                    }
                }

                if (shouldRestartAfterCustomDisabled) {
                    pendingSelectedIndex = null
                    activeSelectedTip = fallbackSelectedTip ?: PcCompactSelectedTip.NoTips
                    restartPaymentWithCurrentAmount("custom tip disabled in settings")
                }
            }
        }
    }

    private suspend fun pausePcEcrForPayment() {
        Log.i(TAG, "ECR pause for SSP payment")
        pcPaymentCommandRepository.pauseForPayment()
            .onSuccess { Log.i(TAG, "ECR paused for SSP payment") }
            .onFailure { Log.e(TAG, "ECR pause before SSP failed", it) }
        delay(PC_USB_SAFETY_SETTLE_DELAY_MS)
    }

    fun selectTipPreset(index: Int) {
        if (isCancelPreviousOperation()) return
        val state = _uiState.value
        if (index !in state.availablePercents.indices) return
        if (!state.isCustomTipSelected && !state.isNoTipsSelected && index == state.selectedPercentIndex) return
        if (!state.canChangeTips) return

        pendingSelectedIndex = index
        _uiState.update {
            it.copy(
                selectedPercentIndex = index,
                isCustomTipSelected = false,
                isNoTipsSelected = false,
                customTipAmount = null,
                errorMessage = null
            )
        }

        tipSelectionDebounceJob?.cancel()
        cancelDeclinedAutoClose()
        tipSelectionDebounceJob = viewModelScope.launch {
            delay(TIP_SELECTION_DEBOUNCE_MS)
            val latest = pendingSelectedIndex ?: return@launch
            pendingSelectedIndex = null
            if (!_uiState.value.canChangeTips) return@launch
            _uiState.update { it.copy(selectedPercentIndex = latest) }
            restartPaymentWithCurrentAmount("tip change")
        }
    }

    fun selectNoTips() {
        if (isCancelPreviousOperation()) return
        val state = _uiState.value
        if (state.isNoTipsSelected || !state.canChangeTips) return
        tipSelectionDebounceJob?.cancel()
        cancelDeclinedAutoClose()
        pendingSelectedIndex = null
        _uiState.update {
            it.copy(
                isNoTipsSelected = true,
                isCustomTipSelected = false,
                customTipAmount = null,
                errorMessage = null
            )
        }
        viewModelScope.launch { restartPaymentWithCurrentAmount("no tips") }
    }

    fun applyCustomTipAmount(amount: Double) {
        if (isCancelPreviousOperation()) return
        val normalized = amount.coerceAtLeast(0.0)
        val state = _uiState.value
        if (!state.showCustomTipButton) return
        if (!state.canChangeTips) return
        if (normalized <= 0.0) return
        if (state.isCustomTipSelected && abs((state.customTipAmount ?: 0.0) - normalized) < 0.01) return
        tipSelectionDebounceJob?.cancel()
        cancelDeclinedAutoClose()
        pendingSelectedIndex = null
        _uiState.update {
            it.copy(
                customTipAmount = normalized,
                isCustomTipSelected = true,
                isNoTipsSelected = false,
                errorMessage = null
            )
        }
        viewModelScope.launch { restartPaymentWithCurrentAmount("custom tip") }
    }

    fun toggleServiceFee(enabled: Boolean) {
        if (isCancelPreviousOperation()) return
        if (!_uiState.value.showServiceFeeToggle) return
        if (_uiState.value.isServiceFeeEnabled == enabled || !_uiState.value.canChangeTips) return
        cancelDeclinedAutoClose()
        _uiState.update { it.copy(isServiceFeeEnabled = enabled, errorMessage = null) }
        viewModelScope.launch { restartPaymentWithCurrentAmount("service fee toggle") }
    }

    fun retryPayment() {
        val state = _uiState.value
        val canRetry = state.paymentStage in setOf(
            CardPresentingStage.Declined,
            CardPresentingStage.Error,
            CardPresentingStage.Cancelled
        )
        if (!canRetry || state.isRestartingPayment || userCancelInProgress) return
        cancelDeclinedAutoClose()

        viewModelScope.launch {
            operationMutex.withLock {
                generation += 1
                val started = if (isCancelPreviousOperation()) {
                    startCancelPreviousPayment("retry", generation)
                } else {
                    startPaymentCurrentAmount("retry", generation)
                }
                if (started) {
                    if (isCancelPreviousOperation()) {
                        activePaymentServiceFeeEnabled = false
                        activeSelectedTip = PcCompactSelectedTip.NoTips
                    } else {
                        activePaymentServiceFeeEnabled = _uiState.value.isServiceFeeEnabled
                        activeSelectedTip = _uiState.value.currentSelectedTip()
                    }
                }
            }
        }
    }

    fun cancelPayment() {
        val state = _uiState.value
        if (!state.canCancel || userCancelInProgress) return
        Log.i(TAG, "User cancel requested operationType=$operationType")

        userCancelInProgress = true
        cancelEventSent = false
        cancelDeclinedAutoClose()
        tipSelectionDebounceJob?.cancel()
        pendingSelectedIndex = null

        _uiState.update {
            it.copy(
                paymentStage = CardPresentingStage.Cancelling,
                canCancel = false,
                isRestartingPayment = false,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            if (isArcus2Source()) {
                val settings = observeSettingsUseCase().first().arcus2NewWaySettings
                sendArcus2StatusNowAwait(
                    statusText = settings.cancellingStatusText,
                    force = true
                )
            }
            val cancelResult = withTimeoutOrNull(USER_CANCEL_TERMINAL_TIMEOUT_MS) { runCatching { cancelPosPaymentUseCase() } }
            if (cancelResult == null) {
                Log.w(TAG, "User cancel timed out, sending final cancelled result anyway")
            } else if (cancelResult.isFailure) {
                Log.w(TAG, "User cancel failed, sending final cancelled result anyway", cancelResult.exceptionOrNull())
            }
            sendCancelledByUserOnce()
        }
    }

    private suspend fun restartPaymentWithCurrentAmount(reason: String) {
        if (isCancelPreviousOperation()) return
        operationMutex.withLock {
            val before = _uiState.value
            if (!before.canChangeTips || userCancelInProgress) return
            cancelDeclinedAutoClose()

            internalRestartInProgress = true
            ignoreNextCancelledFromRestart = true
            _uiState.update { it.copy(isRestartingPayment = true, errorMessage = null) }

            Log.i(TAG, "Restart payment reason=$reason old=${before.totalAmount} new=${_uiState.value.totalAmount}")

            val cancelResult = runCatching { cancelPosPaymentUseCase() }
            if (cancelResult.isFailure) {
                internalRestartInProgress = false
                ignoreNextCancelledFromRestart = false
                Log.e(TAG, "Cancel before restart failed", cancelResult.exceptionOrNull())
                _uiState.update {
                    it.withSelectedTip(activeSelectedTip)
                        .copy(
                            isServiceFeeEnabled = activePaymentServiceFeeEnabled,
                            isRestartingPayment = false,
                            errorMessage = "Не удалось обновить сумму"
                        )
                }
                return
            }

            Log.i(TAG, "Cancel before restart success")
            generation += 1
            val newGeneration = generation
            val started = startPaymentCurrentAmount("restart:$reason", newGeneration)
            if (started) {
                activePaymentServiceFeeEnabled = _uiState.value.isServiceFeeEnabled
                activeSelectedTip = _uiState.value.currentSelectedTip()
            }

            internalRestartInProgress = false
            _uiState.update { curr ->
                curr.copy(isRestartingPayment = false, errorMessage = if (started) null else curr.errorMessage)
            }
        }
    }

    private suspend fun startPaymentCurrentAmount(reason: String, expectedGeneration: Long = generation): Boolean {
        val request = buildRequest(_uiState.value)
        if (request == null) {
            _uiState.update {
                it.copy(
                    paymentStage = CardPresentingStage.Error,
                    isRestartingPayment = false,
                    canCancel = true,
                    errorMessage = "Отсутствуют данные терминала/официанта"
                )
            }
            return false
        }

        Log.i(TAG, "Start SSP payment amount=${request.amount} reason=$reason generation=$expectedGeneration")

        paymentJob?.cancel()
        cancelDeclinedAutoClose()
        _uiState.update { it.copy(paymentStage = CardPresentingStage.Preparing, canCancel = true, errorMessage = null) }

        paymentJob = viewModelScope.launch {
            startPosPaymentUseCase(request).collect { event ->
                if (expectedGeneration != generation) {
                    Log.i(TAG, "Ignore stale payment event generation=$expectedGeneration current=$generation")
                    return@collect
                }
                onPaymentEvent(event)
            }
        }

        return true
    }

    private suspend fun startCancelPreviousPayment(reason: String, expectedGeneration: Long = generation): Boolean {
        Log.i(
            TAG,
            "CANCEL_PREVIOUS start requested reason=$reason generation=$expectedGeneration " +
                "terminalIdBlank=${terminalId.isBlank()} rrnMasked=${maskRrn(sourceRrnNormalized)} " +
                "amount=${_uiState.value.billAmount} currency=$commandCurrency"
        )
        if (terminalId.isBlank()) {
            Log.e(TAG, "Cancel previous operation terminalId missing")
            sendPcEcrFinalResultOnce(PcEcrFinalPaymentResult.Error("Terminal data missing"))
            resumePcEcrAfterPayment("cancel_previous_terminal_missing")
            _events.send(PcCompactTipPaymentEvent.DeclinedTimeout)
            return false
        }
        val rrn = sourceRrnNormalized
        if (rrn.isNullOrBlank()) {
            Log.e(TAG, "Cancel previous operation requested without RRN")
            sendPcEcrFinalResultOnce(PcEcrFinalPaymentResult.Error("RRN missing"))
            resumePcEcrAfterPayment("cancel_previous_rrn_missing")
            _events.send(PcCompactTipPaymentEvent.DeclinedTimeout)
            return false
        }
        val request = PosPaymentCancelPreviousRequest(
            rrn = rrn,
            amount = toMoneyAmount(_uiState.value.billAmount, commandCurrency),
            currency = commandCurrency,
            terminalId = terminalId
        )
        Log.i(TAG, "Start cancel previous SSP op rrn=***${rrn.takeLast(4)} reason=$reason generation=$expectedGeneration")
        paymentJob?.cancel()
        _uiState.update { it.copy(paymentStage = CardPresentingStage.Preparing, canCancel = true, errorMessage = null) }
        paymentJob = viewModelScope.launch {
            Log.i(TAG, "CANCEL_PREVIOUS collecting SSP flow rrnMasked=${maskRrn(rrn)}")
            startPosPaymentCancelPreviousUseCase(request).collect { event ->
                if (expectedGeneration != generation) return@collect
                Log.i(TAG, "CANCEL_PREVIOUS SSP event=${event.javaClass.simpleName} generation=$expectedGeneration current=$generation")
                onPaymentEvent(event)
            }
        }
        return true
    }

    private fun maskRrn(rrn: String?): String =
        rrn?.takeLast(4)?.padStart(rrn.length, '*') ?: "<missing>"

    private suspend fun onPaymentEvent(event: PosPaymentEvent) {
        if (isCancelPreviousOperation()) {
            Log.i(TAG, "CANCEL_PREVIOUS UI stage=${_uiState.value.paymentStage} event=${event.javaClass.simpleName}")
        }
        when (event) {
            PosPaymentEvent.Preparing -> _uiState.update { it.copy(paymentStage = CardPresentingStage.Preparing, canCancel = true) }
            PosPaymentEvent.WaitingForCard -> {
                _uiState.update { it.copy(paymentStage = CardPresentingStage.WaitingForCard, canCancel = true, isRestartingPayment = false) }
                sendArcus2StatusNowForCurrentStage()
            }
            PosPaymentEvent.CardDetected -> {
                _uiState.update { it.copy(paymentStage = CardPresentingStage.CardDetected, canCancel = false, isRestartingPayment = false) }
                sendArcus2StatusNowForCurrentStage()
            }
            PosPaymentEvent.Processing -> {
                _uiState.update { it.copy(paymentStage = CardPresentingStage.Processing, canCancel = false, isRestartingPayment = false) }
                sendArcus2StatusNowForCurrentStage()
            }
            PosPaymentEvent.PinRequired -> {
                _uiState.update { it.copy(paymentStage = CardPresentingStage.PinRequired, canCancel = false, isRestartingPayment = false) }
                sendArcus2StatusNowForCurrentStage()
            }
            is PosPaymentEvent.Approved -> {
                _uiState.update { it.copy(paymentStage = CardPresentingStage.Approved, canCancel = false, isRestartingPayment = false, errorMessage = null) }
                val approvedResult = PcEcrFinalPaymentResult.Approved(
                    message = if (isCancelPreviousOperation()) (event.message ?: "Отмена выполнена") else event.message,
                    externalTransactionId = event.transactionId,
                    rrn = event.rrn,
                    authCode = event.authCode,
                    receiptText = event.receiptText
                )
                if (isArcus2Source()) {
                    sendPcEcrFinalResultOnceWithTimeout(approvedResult, ARCUS2_FINAL_RESULT_SEND_TIMEOUT_MS)
                    resumePcEcrAfterPayment("approved")
                    delay(ARCUS2_RESULT_VISIBLE_MS)
                } else {
                    delay(APPROVED_VISIBLE_MS)
                    sendPcEcrFinalResultOnce(approvedResult)
                    resumePcEcrAfterPayment("approved")
                }
                _events.send(PcCompactTipPaymentEvent.Approved)
            }
            is PosPaymentEvent.Declined -> {
                Log.i(TAG, "Payment declined")
                val fallbackDeclinedText = if (isCancelPreviousOperation()) {
                    "Отмена не выполнена"
                } else {
                    "Оплата отклонена"
                }
                _uiState.update {
                    it.copy(
                        paymentStage = CardPresentingStage.Declined,
                        canCancel = true,
                        isRestartingPayment = false,
                        errorMessage = event.reason?.ifBlank { fallbackDeclinedText } ?: fallbackDeclinedText
                    )
                }
                pendingFinalPcEcrResult = PcEcrFinalPaymentResult.Declined(
                    resultCode = event.code,
                    message = if (isCancelPreviousOperation()) {
                        event.reason?.ifBlank { "Отмена не выполнена" } ?: "Отмена не выполнена"
                    } else {
                        event.reason ?: event.rawMessage
                    },
                    receiptText = event.receiptText
                )
                if (isArcus2Source()) {
                    pendingFinalPcEcrResult?.let { sendPcEcrFinalResultOnceWithTimeout(it, ARCUS2_FINAL_RESULT_SEND_TIMEOUT_MS) }
                    resumePcEcrAfterPayment("declined_arcus2_immediate")
                    delay(ARCUS2_RESULT_VISIBLE_MS)
                    _events.send(PcCompactTipPaymentEvent.DeclinedTimeout)
                } else {
                    scheduleDeclinedAutoClose()
                }
            }
            is PosPaymentEvent.Error -> {
                Log.i(TAG, "Payment error")
                val fallbackErrorText = if (isCancelPreviousOperation()) {
                    "Ошибка отмены"
                } else {
                    "Ошибка оплаты"
                }
                val errorText = event.message.ifBlank { fallbackErrorText }
                _uiState.update {
                    it.copy(
                        paymentStage = CardPresentingStage.Error,
                        canCancel = true,
                        isRestartingPayment = false,
                        errorMessage = errorText
                    )
                }
                pendingFinalPcEcrResult = PcEcrFinalPaymentResult.Error(
                    message = errorText,
                    receiptText = event.receiptText
                )
                if (isArcus2Source()) {
                    pendingFinalPcEcrResult?.let { sendPcEcrFinalResultOnceWithTimeout(it, ARCUS2_FINAL_RESULT_SEND_TIMEOUT_MS) }
                    resumePcEcrAfterPayment("error_arcus2_immediate")
                    delay(ARCUS2_RESULT_VISIBLE_MS)
                    _events.send(PcCompactTipPaymentEvent.DeclinedTimeout)
                } else {
                    scheduleDeclinedAutoClose()
                }
            }
            PosPaymentEvent.Cancelled -> {
                if (ignoreNextCancelledFromRestart) {
                    ignoreNextCancelledFromRestart = false
                    Log.i(TAG, "Ignore cancelled event from restart")
                    return
                }
                if (internalRestartInProgress) {
                    Log.i(TAG, "Ignore cancelled event during internal restart")
                    return
                }
                if (userCancelInProgress) {
                    sendCancelledByUserOnce()
                    return
                }
                _uiState.update {
                    val cancelledText = if (isCancelPreviousOperation()) {
                        "Отмена операции прервана"
                    } else {
                        "Оплата отменена"
                    }
                    it.copy(
                        paymentStage = CardPresentingStage.Cancelled,
                        canCancel = true,
                        isRestartingPayment = false,
                        errorMessage = cancelledText
                    )
                }
                val cancelledText = if (isCancelPreviousOperation()) {
                    "Отмена операции прервана"
                } else {
                    "Cancelled"
                }
                sendPcEcrFinalResultOnceWithTimeout(PcEcrFinalPaymentResult.Cancelled(message = cancelledText), ARCUS2_FINAL_RESULT_SEND_TIMEOUT_MS)
                resumePcEcrAfterPayment("cancelled")
                _events.send(PcCompactTipPaymentEvent.CancelledByUser)
            }
        }
    }

    private suspend fun sendPcEcrFinalResultOnce(result: PcEcrFinalPaymentResult): Boolean {
        if (pcEcrFinalResultSent) {
            Log.i(TAG, "Skip duplicate PC ECR final result send")
            return true
        }
        if (pcEcrFinalResultSending) {
            Log.i(TAG, "Skip duplicate PC ECR final result send while in-progress")
            return false
        }
        pcEcrFinalResultSending = true
        return try {
            stopArcus2StatusKeepAlive()
            val commandId = sourceCommandId?.ifBlank { null }
                ?: sourceOrderId?.ifBlank { null }
                ?: "generated-${System.currentTimeMillis()}"
            val state = _uiState.value
            val frame = paymentResultMapper.map(
                result = result,
                commandId = commandId,
                orderId = sourceOrderId?.ifBlank { null },
                currency = commandCurrency,
                billAmount = toMoneyAmount(state.billAmount, commandCurrency),
                tipAmount = toMoneyAmount(state.selectedTipAmount, commandCurrency),
                totalAmount = toMoneyAmount(state.totalAmount, commandCurrency),
                terminalId = terminalId
            )
            Log.i(TAG, "PC ECR payment result prepared commandId=${frame.commandId} status=${frame.status}")
            val sendResult = if ((sourceProtocol ?: "CHAIOK_JSON") == PcEcrProtocol.ARCUS2_NEWWAY.name) {
                pcPaymentCommandRepository.sendArcus2PaymentResult(
                    sourceCommand = PcPaymentCommand(
                        amount = toMoneyAmount(state.billAmount, commandCurrency),
                        commandId = commandId,
                        currency = commandCurrency,
                        orderId = sourceOrderId?.ifBlank { null },
                        sourceProtocol = PcEcrProtocol.ARCUS2_NEWWAY
                    ),
                    result = result,
                    receiptText = when (result) {
                        is PcEcrFinalPaymentResult.Approved -> result.receiptText
                        is PcEcrFinalPaymentResult.Declined -> result.receiptText
                        is PcEcrFinalPaymentResult.Cancelled -> result.receiptText
                        is PcEcrFinalPaymentResult.Error -> result.receiptText
                    },
                    settings = observeSettingsUseCase().first().arcus2NewWaySettings,
                    terminalId = terminalId
                )
            } else pcPaymentCommandRepository.sendPaymentResult(frame)
            val isArcus = (sourceProtocol ?: "CHAIOK_JSON") == PcEcrProtocol.ARCUS2_NEWWAY.name
            val success = sendResult.isSuccess
            if (success) {
                pcEcrFinalResultSent = true
                if (isArcus) {
                    Log.i(TAG, "PC ARCUS2 payment result sequence sent commandId=${frame.commandId} status=${frame.status}")
                    transactionLogRepository.save(frame, "SENT_ARCUS2", null)
                } else {
                    Log.i(TAG, "PC ECR payment result sent commandId=${frame.commandId} status=${frame.status}")
                    transactionLogRepository.save(frame, "SENT", null)
                }
            } else {
                if (isArcus) {
                    Log.e(TAG, "PC ARCUS2 payment result sequence send failed commandId=${frame.commandId} error=${sendResult.exceptionOrNull()?.message}")
                    transactionLogRepository.save(frame, "FAILED_ARCUS2", sendResult.exceptionOrNull()?.message)
                } else {
                    Log.e(TAG, "PC ECR payment result send failed commandId=${frame.commandId} error=${sendResult.exceptionOrNull()?.message}")
                    transactionLogRepository.save(frame, "FAILED", sendResult.exceptionOrNull()?.message)
                }
            }
            Log.i(TAG, "PC ECR transaction log saved commandId=${frame.commandId}")
            success
        } finally {
            pcEcrFinalResultSending = false
        }
    }

    private fun scheduleDeclinedAutoClose() {
        if (userCancelInProgress || cancelEventSent) return
        if (declinedTimeoutJob?.isActive == true) return
        declinedTimeoutJob?.cancel()
        declinedTimeoutJob = viewModelScope.launch {
            delay(DECLINED_AUTO_CLOSE_DELAY_MS)
            if (!userCancelInProgress && !cancelEventSent) {
                pendingFinalPcEcrResult?.let { sendPcEcrFinalResultOnce(it) }
                resumePcEcrAfterPayment("declined_timeout")
                _events.send(PcCompactTipPaymentEvent.DeclinedTimeout)
            }
        }
    }

    private fun cancelDeclinedAutoClose() {
        declinedTimeoutJob?.cancel()
        declinedTimeoutJob = null
        pendingFinalPcEcrResult = null
    }


    private suspend fun sendCancelledByUserOnce() {
        if (cancelEventSent) return
        cancelDeclinedAutoClose()
        stopArcus2StatusKeepAlive()
        if (isArcus2Source()) {
            val settings = observeSettingsUseCase().first().arcus2NewWaySettings
            if (!(isCancelPreviousOperation() && !settings.sendStatusOnCancelStart)) {
                sendArcus2StatusNowAwait(settings.cancellingStatusText, force = true)
            }
        }
        cancelEventSent = true
        sendPcEcrFinalResultOnceWithTimeout(PcEcrFinalPaymentResult.Cancelled(message = "Cancelled by user"), ARCUS2_FINAL_RESULT_SEND_TIMEOUT_MS)
        stopArcus2StatusKeepAlive()
        resumePcEcrAfterPayment("cancelled_by_user")
        _events.send(PcCompactTipPaymentEvent.CancelledByUser)
    }

    private fun isArcus2Source(): Boolean =
        (sourceProtocol ?: "CHAIOK_JSON") == PcEcrProtocol.ARCUS2_NEWWAY.name

    private fun statusTextForStage(stage: CardPresentingStage, settings: com.chaiok.pos.domain.model.Arcus2NewWaySettings): String =
        if (isCancelPreviousOperation()) when (stage) {
            CardPresentingStage.Preparing, CardPresentingStage.WaitingForCard, CardPresentingStage.Cancelling -> settings.cancellingStatusText
            CardPresentingStage.CardDetected -> settings.cardDetectedStatusText
            CardPresentingStage.Processing -> settings.processingStatusText
            CardPresentingStage.PinRequired -> settings.pinRequiredStatusText
            else -> settings.cancellingStatusText
        } else when (stage) {
            CardPresentingStage.WaitingForCard,
            CardPresentingStage.Preparing -> settings.cardWaitingStatusText
            CardPresentingStage.CardDetected -> settings.cardDetectedStatusText
            CardPresentingStage.Processing -> settings.processingStatusText
            CardPresentingStage.PinRequired -> settings.pinRequiredStatusText
            CardPresentingStage.Cancelling -> settings.cancellingStatusText
            else -> settings.cardWaitingStatusText
        }

    private fun isCancelPreviousOperation(): Boolean =
        operationType == PcEcrOperationType.CANCEL_PREVIOUS

    private fun startArcus2StatusKeepAlive() {
        if (!isArcus2Source()) return
        if (arcus2StatusKeepAliveJob?.isActive == true) return
        arcus2StatusKeepAliveJob = viewModelScope.launch {
            val settings = observeSettingsUseCase().first().arcus2NewWaySettings
            if (!settings.paymentStatusKeepAliveEnabled) return@launch
            if (isCancelPreviousOperation() && !settings.cancelStatusKeepAliveEnabled) return@launch
            while (isActive && !pcEcrFinalResultSent && !userCancelInProgress) {
                val statusText = statusTextForStage(_uiState.value.paymentStage, settings)
                pcPaymentCommandRepository.sendArcus2StatusIfActive(statusText, settings)
                delay(settings.paymentStatusKeepAliveIntervalMs.coerceAtLeast(3_000L))
            }
        }
    }

    private fun stopArcus2StatusKeepAlive() {
        arcus2StatusKeepAliveJob?.cancel()
        arcus2StatusKeepAliveJob = null
    }

    private fun sendArcus2StatusNowForCurrentStage() {
        if (!isArcus2Source()) return
        viewModelScope.launch {
            val settings = observeSettingsUseCase().first().arcus2NewWaySettings
            if (isCancelPreviousOperation() && !settings.cancelStatusKeepAliveEnabled) return@launch
            sendArcus2StatusNow(statusTextForStage(_uiState.value.paymentStage, settings))
        }
    }

    private fun sendArcus2StatusNow(statusText: String) {
        if (!isArcus2Source()) return
        if (lastArcus2StatusText == statusText) return
        lastArcus2StatusText = statusText
        viewModelScope.launch {
            val settings = observeSettingsUseCase().first().arcus2NewWaySettings
            pcPaymentCommandRepository.sendArcus2StatusIfActive(statusText, settings)
        }
    }

    private suspend fun sendArcus2StatusNowAwait(
        statusText: String,
        force: Boolean = false
    ) {
        if (!isArcus2Source()) return
        if (!force && lastArcus2StatusText == statusText) return
        lastArcus2StatusText = statusText
        val settings = observeSettingsUseCase().first().arcus2NewWaySettings
        if (isCancelPreviousOperation() && !settings.cancelStatusKeepAliveEnabled) return
        val result = pcPaymentCommandRepository.sendArcus2StatusIfActive(statusText, settings)
        result.onFailure {
            Log.w(TAG, "ARCUS2 immediate STATUS failed text=$statusText error=${it.message}", it)
        }
    }

    private suspend fun sendPcEcrFinalResultOnceWithTimeout(
        result: PcEcrFinalPaymentResult,
        timeoutMs: Long
    ): Boolean {
        if (!isArcus2Source()) {
            return sendPcEcrFinalResultOnce(result)
        }
        Log.i(TAG, "ARCUS2 final result send started (timeoutMs=$timeoutMs handled per-step in repository)")
        val success = sendPcEcrFinalResultOnce(result)
        if (success) Log.i(TAG, "ARCUS2 final result send success")
        else Log.w(TAG, "ARCUS2 final result send failed")
        return success
    }

    private suspend fun resumePcEcrAfterPayment(reason: String) {
        Log.i(TAG, "ECR resume after SSP payment reason=$reason")
        val result = withTimeoutOrNull(1_200L) {
            pcPaymentCommandRepository.resumeAfterPayment()
        }
        if (result == null) {
            Log.w(TAG, "ECR resume after SSP payment timed out")
            return
        }
        result.onSuccess { Log.i(TAG, "ECR resumed listening") }
            .onFailure { Log.e(TAG, "ECR resume after SSP payment failed", it) }
    }

    private fun toMoneyAmount(value: Double, currency: String): BigDecimal =
        when (currency.uppercase()) {
            "AMD" -> BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP)
            else -> BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        }

    private fun buildRequest(state: PcCompactTipPaymentUiState): PosPaymentRequest? {
        if (waiterId.isBlank() || terminalId.isBlank()) return null
        return PosPaymentRequest(
            amount = toMoneyAmount(state.totalAmount, commandCurrency),
            waiterId = waiterId,
            terminalId = terminalId,
            tipAmount = state.selectedTipAmount,
            serviceFee = state.serviceFeeAmount,
            feesCovered = state.showServiceFeeToggle && state.isServiceFeeEnabled
        )
    }

    private fun resolveDefaultIndex(percents: List<Double>, defaultIndex: Int?): Int {
        val fromRange = defaultIndex?.takeIf { it in percents.indices }
        if (fromRange != null) return fromRange
        val tenIndex = percents.indexOfFirst { it == 10.0 }
        if (tenIndex >= 0) return tenIndex
        val nonZero = percents.indexOfFirst { it > 0.0 }
        return if (nonZero >= 0) nonZero else 0
    }

    override fun onCleared() {
        stopArcus2StatusKeepAlive()
        tipSelectionDebounceJob?.cancel()
        paymentJob?.cancel()
        cancelDeclinedAutoClose()

        val stage = _uiState.value.paymentStage
        val active = stage in setOf(
            CardPresentingStage.Preparing,
            CardPresentingStage.WaitingForCard,
            CardPresentingStage.CardDetected,
            CardPresentingStage.Processing,
            CardPresentingStage.PinRequired
        )

        cleanupScope.launch {
            if (active && !userCancelInProgress) {
                runCatching { cancelPosPaymentUseCase() }
                    .onFailure { Log.e(TAG, "Cancel onCleared failed", it) }
            }

            runCatching {
                withTimeoutOrNull(1_200L) {
                    pcPaymentCommandRepository.resumeAfterPayment()
                }
            }.onFailure { Log.e(TAG, "ECR resume onCleared failed", it) }

            cleanupScope.cancel()
        }

        super.onCleared()
    }

    companion object {
        private const val TAG = "PcCompactTipPayment"
        private const val PC_USB_SAFETY_SETTLE_DELAY_MS = 150L
        private const val TIP_SELECTION_DEBOUNCE_MS = 300L
        private const val APPROVED_VISIBLE_MS = 1200L
        private const val ARCUS2_RESULT_VISIBLE_MS = 900L
        private const val ARCUS2_FINAL_RESULT_SEND_TIMEOUT_MS = 3_000L
        private const val DECLINED_AUTO_CLOSE_DELAY_MS = 10_000L
        private const val USER_CANCEL_TERMINAL_TIMEOUT_MS = 2_500L
    }
}

private sealed interface PcCompactSelectedTip {
    data class Percent(val index: Int) : PcCompactSelectedTip
    data class CustomAmount(val amount: Double) : PcCompactSelectedTip
    object NoTips : PcCompactSelectedTip
}

private fun PcCompactTipPaymentUiState.currentSelectedTip(): PcCompactSelectedTip = when {
    isNoTipsSelected -> PcCompactSelectedTip.NoTips
    isCustomTipSelected -> PcCompactSelectedTip.CustomAmount(customTipAmount ?: 0.0)
    else -> PcCompactSelectedTip.Percent(selectedPercentIndex)
}

private fun PcCompactTipPaymentUiState.withSelectedTip(tip: PcCompactSelectedTip): PcCompactTipPaymentUiState {
    return when (tip) {
        is PcCompactSelectedTip.Percent -> copy(
            selectedPercentIndex = if (tip.index in availablePercents.indices) tip.index else 0,
            customTipAmount = null,
            isCustomTipSelected = false,
            isNoTipsSelected = false
        )
        is PcCompactSelectedTip.CustomAmount -> copy(
            selectedPercentIndex = selectedPercentIndex.coerceIn(0, availablePercents.lastIndex.coerceAtLeast(0)),
            customTipAmount = tip.amount,
            isCustomTipSelected = true,
            isNoTipsSelected = false
        )
        PcCompactSelectedTip.NoTips -> copy(
            customTipAmount = null,
            isCustomTipSelected = false,
            isNoTipsSelected = true
        )
    }
}

internal fun formatRubles(amount: Double): String =
    formatKopecks(amountToKopecks(amount))

private fun amountToKopecks(amount: Double): Int =
    (amount * 100.0).roundToInt()

private fun formatKopecks(kopecks: Int): String {
    val absKopecks = abs(kopecks)
    val rubles = absKopecks / 100
    val cents = absKopecks % 100
    val groupedRubles = "%,d".format(rubles).replace(',', ' ')
    val prefix = if (kopecks < 0) "-" else ""
    return if (cents == 0) {
        "$prefix$groupedRubles ₽"
    } else {
        "$prefix$groupedRubles,${cents.toString().padStart(2, '0')} ₽"
    }
}

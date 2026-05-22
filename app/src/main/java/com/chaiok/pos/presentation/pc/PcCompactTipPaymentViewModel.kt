package com.chaiok.pos.presentation.pc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.repository.PcPaymentCommandRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.usecase.CancelPosPaymentUseCase
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
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
import kotlinx.coroutines.sync.Mutex
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
    val serviceFeePercent: Double = 0.0,
    val showServiceFeeToggle: Boolean = true,
    val isServiceFeeEnabled: Boolean = false,
    val paymentStage: CardPresentingStage = CardPresentingStage.Idle,
    val isRestartingPayment: Boolean = false,
    val errorMessage: String? = null,
    val canCancel: Boolean = true
) {
    fun calculateTipByPercent(percent: Double): Double =
        roundMoney(billAmount * percent / 100.0)

    val selectedTipAmount: Double
        get() = when {
            isNoTipsSelected -> 0.0
            isCustomTipSelected -> roundMoney(customTipAmount ?: 0.0)
            else -> availablePercents
                .getOrNull(selectedPercentIndex)
                ?.let(::calculateTipByPercent)
                ?: 0.0
        }

    val serviceFeeAmount: Double
        get() = if (showServiceFeeToggle && isServiceFeeEnabled && serviceFeePercent > 0.0) roundMoney(selectedTipAmount * serviceFeePercent / 100.0) else 0.0

    val totalAmount: Double
        get() = billAmount + selectedTipAmount + serviceFeeAmount

    val amountText: String
        get() = formatRubles(totalAmount)

    val canChangeTips: Boolean
        get() = !isRestartingPayment && paymentStage in setOf(
            CardPresentingStage.Idle,
            CardPresentingStage.Preparing,
            CardPresentingStage.WaitingForCard
        )

    private fun roundMoney(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}

sealed interface PcCompactTipPaymentEvent {
    data object Approved : PcCompactTipPaymentEvent
    data object CancelledByUser : PcCompactTipPaymentEvent
}

class PcCompactTipPaymentViewModel(
    private val billAmount: Double,
    private val startPosPaymentUseCase: StartPosPaymentUseCase,
    private val cancelPosPaymentUseCase: CancelPosPaymentUseCase,
    private val getTransactionRangeUseCase: GetTransactionRangeUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val sessionRepository: SessionRepository,
    private val pcPaymentCommandRepository: PcPaymentCommandRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PcCompactTipPaymentUiState(billAmount = billAmount))
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<PcCompactTipPaymentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val operationMutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var paymentJob: Job? = null
    private var tipSelectionDebounceJob: Job? = null
    private var pendingSelectedIndex: Int? = null
    private var activeSelectedTip: PcCompactSelectedTip = PcCompactSelectedTip.Percent(0)

    private var generation = 0L
    private var internalRestartInProgress = false
    private var userCancelInProgress = false
    private var cancelEventSent = false
    private var ignoreNextCancelledFromRestart = false

    private var waiterId: String = ""
    private var activePaymentServiceFeeEnabled: Boolean = false
    private var terminalId: String = ""

    init {
        viewModelScope.launch { initStateAndStart() }
    }

    private suspend fun initStateAndStart() {
        terminalId = sessionRepository.terminalId.first().orEmpty()
        val profile = observeProfileUseCase().filterNotNull().first()
        waiterId = profile.id

        val range = getTransactionRangeUseCase.observe().first()
        val settings = observeSettingsUseCase().first()
        val percents = range?.percents?.takeIf { it.isNotEmpty() } ?: listOf(5.0, 10.0, 15.0)
        val selected = resolveDefaultIndex(percents, range?.defaultIndex)

        _uiState.update {
            it.copy(
                availablePercents = percents,
                selectedPercentIndex = selected,
                serviceFeePercent = profile.serviceFeePercent.coerceAtLeast(0.0),
                showServiceFeeToggle = settings.pcCompactServiceFeeEnabled,
                isServiceFeeEnabled = false,
                paymentStage = CardPresentingStage.Preparing
            )
        }

        releasePcUsbBeforePayment()

        generation += 1
        val started = startPaymentCurrentAmount("initial", generation)
        if (started) {
            activePaymentServiceFeeEnabled = _uiState.value.isServiceFeeEnabled
            activeSelectedTip = _uiState.value.currentSelectedTip()
        }
    }

    private suspend fun releasePcUsbBeforePayment() {
        Log.i(TAG, "Release ECR before SSP start")
        runCatching { pcPaymentCommandRepository.stop() }
            .onSuccess { Log.i(TAG, "Release ECR before SSP done") }
            .onFailure { Log.e(TAG, "Release ECR before SSP failed", it) }
        delay(PC_USB_SAFETY_SETTLE_DELAY_MS)
    }

    fun selectTipPreset(index: Int) {
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
        val state = _uiState.value
        if (state.isNoTipsSelected || !state.canChangeTips) return
        tipSelectionDebounceJob?.cancel()
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
        val normalized = amount.coerceAtLeast(0.0)
        val state = _uiState.value
        if (!state.canChangeTips) return
        if (normalized <= 0.0) return
        if (state.isCustomTipSelected && abs((state.customTipAmount ?: 0.0) - normalized) < 0.01) return
        tipSelectionDebounceJob?.cancel()
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
        if (!_uiState.value.showServiceFeeToggle) return
        if (_uiState.value.isServiceFeeEnabled == enabled || !_uiState.value.canChangeTips) return
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

        viewModelScope.launch {
            operationMutex.withLock {
                generation += 1
                val started = startPaymentCurrentAmount("retry", generation)
                if (started) {
                    activePaymentServiceFeeEnabled = _uiState.value.isServiceFeeEnabled
                    activeSelectedTip = _uiState.value.currentSelectedTip()
                }
            }
        }
    }

    fun cancelPayment() {
        val state = _uiState.value
        if (!state.canCancel || userCancelInProgress) return

        userCancelInProgress = true
        cancelEventSent = false
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
            val result = runCatching { cancelPosPaymentUseCase() }
            result.onSuccess {
                sendCancelledByUserOnce()
            }.onFailure {
                userCancelInProgress = false
                cancelEventSent = false
                Log.e(TAG, "User cancel failed", it)
                _uiState.update { curr ->
                    curr.copy(
                        paymentStage = CardPresentingStage.WaitingForCard,
                        canCancel = true,
                        errorMessage = "Не удалось отменить оплату"
                    )
                }
            }
        }
    }

    private suspend fun restartPaymentWithCurrentAmount(reason: String) {
        operationMutex.withLock {
            val before = _uiState.value
            if (!before.canChangeTips || userCancelInProgress) return

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

    private suspend fun onPaymentEvent(event: PosPaymentEvent) {
        when (event) {
            PosPaymentEvent.Preparing -> _uiState.update { it.copy(paymentStage = CardPresentingStage.Preparing, canCancel = true) }
            PosPaymentEvent.WaitingForCard -> _uiState.update { it.copy(paymentStage = CardPresentingStage.WaitingForCard, canCancel = true, isRestartingPayment = false) }
            PosPaymentEvent.CardDetected -> _uiState.update { it.copy(paymentStage = CardPresentingStage.CardDetected, canCancel = false, isRestartingPayment = false) }
            PosPaymentEvent.Processing -> _uiState.update { it.copy(paymentStage = CardPresentingStage.Processing, canCancel = false, isRestartingPayment = false) }
            PosPaymentEvent.PinRequired -> _uiState.update { it.copy(paymentStage = CardPresentingStage.PinRequired, canCancel = false, isRestartingPayment = false) }
            is PosPaymentEvent.Approved -> {
                _uiState.update { it.copy(paymentStage = CardPresentingStage.Approved, canCancel = false, isRestartingPayment = false, errorMessage = null) }
                delay(APPROVED_VISIBLE_MS)
                _events.send(PcCompactTipPaymentEvent.Approved)
            }
            is PosPaymentEvent.Declined -> {
                Log.i(TAG, "Payment declined")
                _uiState.update {
                    it.copy(
                        paymentStage = CardPresentingStage.Declined,
                        canCancel = true,
                        isRestartingPayment = false,
                        errorMessage = event.reason ?: "Оплата отклонена"
                    )
                }
            }
            is PosPaymentEvent.Error -> {
                Log.i(TAG, "Payment error")
                _uiState.update {
                    it.copy(
                        paymentStage = CardPresentingStage.Error,
                        canCancel = true,
                        isRestartingPayment = false,
                        errorMessage = event.message.ifBlank { "Ошибка оплаты" }
                    )
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
                    it.copy(
                        paymentStage = CardPresentingStage.Cancelled,
                        canCancel = true,
                        isRestartingPayment = false,
                        errorMessage = "Оплата отменена"
                    )
                }
            }
        }
    }


    private suspend fun sendCancelledByUserOnce() {
        if (cancelEventSent) return
        cancelEventSent = true
        _events.send(PcCompactTipPaymentEvent.CancelledByUser)
    }

    private fun buildRequest(state: PcCompactTipPaymentUiState): PosPaymentRequest? {
        if (waiterId.isBlank() || terminalId.isBlank()) return null
        return PosPaymentRequest(
            amount = BigDecimal.valueOf(state.totalAmount).setScale(2, RoundingMode.HALF_UP),
            waiterId = waiterId,
            terminalId = terminalId,
            tipAmount = state.selectedTipAmount,
            serviceFee = state.serviceFeeAmount,
            feesCovered = state.isServiceFeeEnabled
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
        tipSelectionDebounceJob?.cancel()
        paymentJob?.cancel()

        val stage = _uiState.value.paymentStage
        val active = stage in setOf(
            CardPresentingStage.Preparing,
            CardPresentingStage.WaitingForCard,
            CardPresentingStage.CardDetected,
            CardPresentingStage.Processing,
            CardPresentingStage.PinRequired
        )

        if (active && !userCancelInProgress) {
            cleanupScope.launch {
                runCatching { cancelPosPaymentUseCase() }
                    .onFailure { Log.e(TAG, "Cancel onCleared failed", it) }
                cleanupScope.cancel()
            }
        } else {
            cleanupScope.cancel()
        }

        super.onCleared()
    }

    companion object {
        private const val TAG = "PcCompactTipPayment"
        private const val PC_USB_SAFETY_SETTLE_DELAY_MS = 150L
        private const val TIP_SELECTION_DEBOUNCE_MS = 300L
        private const val APPROVED_VISIBLE_MS = 1200L
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

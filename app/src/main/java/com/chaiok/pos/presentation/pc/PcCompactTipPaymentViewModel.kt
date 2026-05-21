package com.chaiok.pos.presentation.pc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.PaymentResult
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.model.toPaymentResultOrNull
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.usecase.CancelPosPaymentUseCase
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.StartPosPaymentUseCase
import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.Job
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
import kotlin.math.roundToInt

data class PcCompactTipPaymentUiState(
    val billAmount: Double,
    val availablePercents: List<Double> = emptyList(),
    val selectedPercentIndex: Int = 0,
    val serviceFeePercent: Double = 0.0,
    val isServiceFeeEnabled: Boolean = false,
    val paymentStage: CardPresentingStage = CardPresentingStage.Idle,
    val isRestartingPayment: Boolean = false,
    val errorMessage: String? = null,
    val canCancel: Boolean = true
) {
    val selectedTipAmount: Double
        get() = roundMoney(billAmount * (availablePercents.getOrNull(selectedPercentIndex) ?: 0.0) / 100.0)

    val serviceFeeAmount: Double
        get() = if (isServiceFeeEnabled && serviceFeePercent > 0.0) roundMoney(selectedTipAmount * serviceFeePercent / 100.0) else 0.0

    val totalAmount: Double
        get() = billAmount + selectedTipAmount + serviceFeeAmount

    val amountText: String
        get() = "%.2f ₽".format(totalAmount)

    val canChangeTips: Boolean
        get() = !isRestartingPayment && paymentStage in setOf(
            CardPresentingStage.Idle,
            CardPresentingStage.Preparing,
            CardPresentingStage.WaitingForCard
        )

    private fun roundMoney(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}

sealed interface PcCompactTipPaymentEvent {
    data class Finished(val result: PaymentResult) : PcCompactTipPaymentEvent
    data object CancelledByUser : PcCompactTipPaymentEvent
}

class PcCompactTipPaymentViewModel(
    private val billAmount: Double,
    private val startPosPaymentUseCase: StartPosPaymentUseCase,
    private val cancelPosPaymentUseCase: CancelPosPaymentUseCase,
    private val getTransactionRangeUseCase: GetTransactionRangeUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PcCompactTipPaymentUiState(billAmount = billAmount))
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<PcCompactTipPaymentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val restartMutex = Mutex()
    private var paymentJob: Job? = null
    private var generation = 0L
    private var restartCancellingGeneration: Long? = null
    private var pendingSelectedIndex: Int? = null

    private var waiterId: String = ""
    private var terminalId: String = ""

    init {
        viewModelScope.launch { initStateAndStart() }
    }

    private suspend fun initStateAndStart() {
        terminalId = sessionRepository.terminalId.first().orEmpty()
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
                isServiceFeeEnabled = false,
                paymentStage = CardPresentingStage.Preparing
            )
        }

        startPaymentCurrentAmount("initial")
    }

    fun selectTipPreset(index: Int) {
        if (index == _uiState.value.selectedPercentIndex || !_uiState.value.canChangeTips) return
        pendingSelectedIndex = index
        viewModelScope.launch {
            delay(300)
            val latest = pendingSelectedIndex ?: return@launch
            pendingSelectedIndex = null
            _uiState.update { it.copy(selectedPercentIndex = latest) }
            restartPaymentWithCurrentAmount("tip change")
        }
    }

    fun toggleServiceFee(enabled: Boolean) {
        if (_uiState.value.isServiceFeeEnabled == enabled || !_uiState.value.canChangeTips) return
        _uiState.update { it.copy(isServiceFeeEnabled = enabled) }
        viewModelScope.launch { restartPaymentWithCurrentAmount("service fee toggle") }
    }

    fun retryPayment() {
        viewModelScope.launch { startPaymentCurrentAmount("retry") }
    }

    fun cancelPayment() {
        if (!_uiState.value.canCancel) return
        _uiState.update { it.copy(paymentStage = CardPresentingStage.Cancelling, canCancel = false) }
        viewModelScope.launch {
            cancelPosPaymentUseCase()
            _events.send(PcCompactTipPaymentEvent.CancelledByUser)
        }
    }

    private suspend fun restartPaymentWithCurrentAmount(reason: String) {
        restartMutex.withLock {
            if (!_uiState.value.canChangeTips) return
            _uiState.update { it.copy(isRestartingPayment = true, errorMessage = null) }
            val oldAmount = _uiState.value.totalAmount
            generation += 1
            val currentGeneration = generation
            restartCancellingGeneration = currentGeneration

            Log.i("PcCompactTipPayment", "Restart payment due to $reason old=$oldAmount new=${_uiState.value.totalAmount}")

            val cancelResult = runCatching { cancelPosPaymentUseCase() }
            if (cancelResult.isFailure) {
                Log.e("PcCompactTipPayment", "Cancel before restart failure", cancelResult.exceptionOrNull())
                _uiState.update { it.copy(isRestartingPayment = false, errorMessage = "Не удалось обновить сумму") }
                return
            }

            Log.i("PcCompactTipPayment", "Cancel before restart success")
            startPaymentCurrentAmount("restart", currentGeneration)
            _uiState.update { it.copy(isRestartingPayment = false) }
        }
    }

    private suspend fun startPaymentCurrentAmount(reason: String, expectedGeneration: Long = generation) {
        val request = buildRequest(_uiState.value)
        if (request == null) {
            _uiState.update { it.copy(errorMessage = "Отсутствуют данные терминала/официанта") }
            return
        }

        Log.i("PcCompactTipPayment", "PcCompactTipPayment start amount=${request.amount} reason=$reason")

        _uiState.update { it.copy(paymentStage = CardPresentingStage.Preparing, canCancel = true, errorMessage = null) }
        paymentJob?.cancel()
        paymentJob = viewModelScope.launch {
            startPosPaymentUseCase(request).collect { event ->
                if (expectedGeneration != generation) return@collect
                onPaymentEvent(event, expectedGeneration)
            }
        }
    }

    private suspend fun onPaymentEvent(event: PosPaymentEvent, eventGeneration: Long) {
        when (event) {
            PosPaymentEvent.Preparing -> _uiState.update { it.copy(paymentStage = CardPresentingStage.Preparing) }
            PosPaymentEvent.WaitingForCard -> _uiState.update { it.copy(paymentStage = CardPresentingStage.WaitingForCard) }
            PosPaymentEvent.CardDetected -> _uiState.update { it.copy(paymentStage = CardPresentingStage.CardDetected) }
            PosPaymentEvent.Processing -> _uiState.update { it.copy(paymentStage = CardPresentingStage.Processing, canCancel = false) }
            PosPaymentEvent.PinRequired -> _uiState.update { it.copy(paymentStage = CardPresentingStage.PinRequired, canCancel = false) }
            is PosPaymentEvent.Approved,
            is PosPaymentEvent.Declined,
            is PosPaymentEvent.Error -> {
                val result = event.toPaymentResultOrNull() ?: return
                _events.send(PcCompactTipPaymentEvent.Finished(result))
            }
            PosPaymentEvent.Cancelled -> {
                if (restartCancellingGeneration == eventGeneration) {
                    restartCancellingGeneration = null
                    return
                }
                _events.send(PcCompactTipPaymentEvent.CancelledByUser)
            }
        }
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
}

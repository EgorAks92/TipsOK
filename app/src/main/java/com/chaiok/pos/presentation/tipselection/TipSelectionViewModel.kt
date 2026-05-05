package com.chaiok.pos.presentation.tipselection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.PaymentResult
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.usecase.AddReviewUseCase
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.presentation.background.WaiterBackgroundMemoryCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal fun normalizeEvaluation(value: Int): Int = value.coerceIn(1, 5)

sealed interface TipPaymentUiState {
    data object Idle : TipPaymentUiState
    data object Processing : TipPaymentUiState
    data class Approved(val message: String? = null) : TipPaymentUiState
    data class Declined(val message: String? = null) : TipPaymentUiState
}

data class TipSelectionUiState(
    val isLoading: Boolean = true,
    val billAmount: Double = 0.0,
    val availablePercents: List<Double> = emptyList(),
    val selectedPercentIndex: Int = 0,
    val customTipAmount: Double? = null,
    val isCustomSelected: Boolean = false,
    val waiterId: String = "",
    val waiterName: String = "Ваш официант",
    val waiterStatus: String = "Коплю на отпуск!",
    val tileBackground: String? = WaiterBackgroundMemoryCache.currentBackground,
    val terminalId: String = "",
    val serviceFeePercent: Double = 0.0,
    val isServiceFeeEnabled: Boolean = false,
    val showCustomTipDialog: Boolean = false,
    val errorMessage: String? = null,
    val paymentState: TipPaymentUiState = TipPaymentUiState.Idle,
    val kitchenEvaluation: Int = 0,
    val serviceEvaluation: Int = 0
) {
    fun calculateTipByPercent(percent: Double): Double =
        roundMoney(billAmount * percent / 100.0)

    val selectedTipAmount: Double
        get() = if (isCustomSelected) {
            customTipAmount ?: 0.0
        } else {
            availablePercents
                .getOrNull(selectedPercentIndex)
                ?.let(::calculateTipByPercent)
                ?: 0.0
        }

    val serviceFeeAmount: Double
        get() = if (isServiceFeeEnabled && serviceFeePercent > 0.0) {
            roundMoney(selectedTipAmount * serviceFeePercent / 100.0)
        } else {
            0.0
        }

    val totalAmount: Double
        get() = billAmount + selectedTipAmount + serviceFeeAmount

    val isPayEnabled: Boolean
        get() = !isLoading &&
                totalAmount > 0 &&
                paymentState != TipPaymentUiState.Processing

    companion object {
        private fun roundMoney(value: Double): Double =
            (value * 100.0).roundToInt() / 100.0
    }
}

class TipSelectionViewModel(
    private val billAmount: Double,
    private val getTransactionRangeUseCase: GetTransactionRangeUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val addReviewUseCase: AddReviewUseCase,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private var reviewSentForCurrentPayment = false

    private val _uiState = MutableStateFlow(
        TipSelectionUiState(
            billAmount = billAmount,
            tileBackground = WaiterBackgroundMemoryCache.currentBackground
        )
    )
    val uiState: StateFlow<TipSelectionUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        loadInitialDataAndObserveTipRange()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                val background = settings.tileBackground.ifBlank { "default" }

                WaiterBackgroundMemoryCache.setCurrentBackground(background)

                _uiState.update {
                    it.copy(tileBackground = background)
                }
            }
        }
    }

    private fun loadInitialDataAndObserveTipRange() {
        viewModelScope.launch {
            val terminalId = sessionRepository.terminalId.first().orEmpty()
            val profile = observeProfileUseCase().filterNotNull().first()

            val waiterName = listOf(profile.firstName, profile.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Ваш официант" }

            val waiterStatus = profile.status.ifBlank { "Коплю на отпуск!" }
            val serviceFeePercent = profile.serviceFeePercent.coerceAtLeast(0.0)

            _uiState.update {
                it.copy(
                    waiterId = profile.id,
                    waiterName = waiterName,
                    waiterStatus = waiterStatus,
                    terminalId = terminalId,
                    serviceFeePercent = serviceFeePercent,
                    isServiceFeeEnabled = false
                )
            }

            val fallbackPercents = listOf(5.0, 10.0, 15.0)
            val fallbackDefaultIndex = 1

            getTransactionRangeUseCase.observe().collect { range ->
                val percents = range?.percents?.takeIf { it.isNotEmpty() } ?: fallbackPercents
                val defaultIndex = range?.defaultIndex ?: fallbackDefaultIndex

                _uiState.update {
                    val shouldUseDefaultIndex = it.availablePercents.isEmpty()

                    val nextSelectedIndex = if (shouldUseDefaultIndex) {
                        defaultIndex.coerceIn(0, percents.lastIndex)
                    } else {
                        it.selectedPercentIndex.coerceIn(0, percents.lastIndex)
                    }

                    it.copy(
                        isLoading = false,
                        availablePercents = percents,
                        selectedPercentIndex = nextSelectedIndex
                    )
                }
            }
        }
    }

    fun selectPreset(index: Int) {
        _uiState.update {
            it.copy(
                selectedPercentIndex = index,
                isCustomSelected = false,
                customTipAmount = null
            )
        }
    }

    fun openCustomDialog() {
        _uiState.update {
            it.copy(showCustomTipDialog = true)
        }
    }

    fun dismissCustomDialog() {
        _uiState.update {
            it.copy(showCustomTipDialog = false)
        }
    }

    fun applyCustom(amount: String) {
        _uiState.update {
            it.copy(
                customTipAmount = amount.toDoubleOrNull() ?: 0.0,
                isCustomSelected = true,
                showCustomTipDialog = false
            )
        }
    }

    fun toggleServiceFee(enabled: Boolean) {
        _uiState.update {
            it.copy(isServiceFeeEnabled = enabled)
        }
    }

    fun selectKitchenEvaluation(value: Int) {
        _uiState.update {
            it.copy(kitchenEvaluation = normalizeEvaluation(value))
        }
    }

    fun selectServiceEvaluation(value: Int) {
        _uiState.update {
            it.copy(serviceEvaluation = normalizeEvaluation(value))
        }
    }

    fun startExternalPayment(): Boolean {
        val current = _uiState.value

        if (current.paymentState == TipPaymentUiState.Processing) {
            return false
        }

        if (current.totalAmount <= 0.0) {
            _uiState.update {
                it.copy(errorMessage = "Сумма должна быть больше 0")
            }
            return false
        }

        if (current.waiterId.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Не удалось получить ID официанта")
            }
            return false
        }

        if (current.terminalId.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Не удалось получить ID терминала")
            }
            return false
        }

        if (current.kitchenEvaluation !in 1..5 || current.serviceEvaluation !in 1..5) {
            _uiState.update {
                it.copy(errorMessage = "Оцените кухню и сервис")
            }
            return false
        }

        reviewSentForCurrentPayment = false

        _uiState.update {
            it.copy(paymentState = TipPaymentUiState.Processing)
        }

        return true
    }

    private fun sendReview(
        kitchenEvaluation: Int,
        serviceEvaluation: Int
    ) {
        viewModelScope.launch {
            addReviewUseCase(
                kitchenEvaluation = kitchenEvaluation,
                serviceEvaluation = serviceEvaluation,
                comment = ""
            )
                .onSuccess {
                    Log.i(
                        REVIEW_TAG,
                        "addReview success"
                    )
                }
                .onFailure { error ->
                    Log.e(
                        REVIEW_TAG,
                        "addReview failed but payment flow continues",
                        error
                    )
                }
        }
    }

    fun handlePaymentResult(result: PaymentResult) {
        when (result) {
            is PaymentResult.Approved -> {
                Log.i(
                    REVIEW_TAG,
                    "handlePaymentResult Approved rawMessagePreview=${result.rawMessage.toPaymentMessagePreview()}"
                )

                if (!reviewSentForCurrentPayment) {
                    reviewSentForCurrentPayment = true

                    sendReview(
                        kitchenEvaluation = _uiState.value.kitchenEvaluation,
                        serviceEvaluation = _uiState.value.serviceEvaluation
                    )
                }

                _uiState.update {
                    it.copy(
                        paymentState = TipPaymentUiState.Approved(
                            result.rawMessage ?: "Оплата одобрена"
                        )
                    )
                }
            }

            is PaymentResult.Declined -> {
                Log.i(
                    REVIEW_TAG,
                    "handlePaymentResult Declined reasonPreview=${result.reason.toPaymentMessagePreview()}"
                )

                _uiState.update {
                    it.copy(
                        paymentState = TipPaymentUiState.Declined(
                            result.reason ?: "Оплата отклонена"
                        )
                    )
                }
            }

            is PaymentResult.Error -> {
                Log.i(
                    REVIEW_TAG,
                    "handlePaymentResult Error messagePreview=${result.message.toPaymentMessagePreview()}"
                )

                _uiState.update {
                    it.copy(
                        paymentState = TipPaymentUiState.Declined(result.message)
                    )
                }
            }
        }
    }

    fun handlePaymentLaunchError(message: String) {
        _uiState.update {
            it.copy(paymentState = TipPaymentUiState.Declined(message))
        }
    }

    fun resetPaymentState() {
        _uiState.update {
            it.copy(paymentState = TipPaymentUiState.Idle)
        }
    }

    fun onMessageShown() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private companion object {
        private const val REVIEW_TAG = "TipsReviewFlow"
    }
}

private fun String?.toPaymentMessagePreview(): String {
    val normalized = this
        ?.replace("\n", " ")
        ?.replace("\r", " ")
        ?.trim()
        ?.take(160)

    return if (normalized.isNullOrBlank()) {
        "<blank>"
    } else {
        "\"$normalized\""
    }
}
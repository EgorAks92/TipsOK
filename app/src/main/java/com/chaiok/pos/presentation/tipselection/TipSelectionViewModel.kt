package com.chaiok.pos.presentation.tipselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class TipSelectionUiState(
    val isLoading: Boolean = true,
    val billAmount: Double = 0.0,
    val availablePercents: List<Double> = emptyList(),
    val selectedPercentIndex: Int = 0,
    val customTipAmount: Double? = null,
    val isCustomSelected: Boolean = false,
    val waiterName: String = "Ваш официант",
    val waiterStatus: String = "Коплю на отпуск!",
    val showCustomTipDialog: Boolean = false,
    val errorMessage: String? = null
) {
    fun calculateTipByPercent(percent: Double): Double = ((billAmount * percent / 100.0) * 100.0).roundToInt() / 100.0
    val selectedTipAmount: Double get() = if (isCustomSelected) (customTipAmount ?: 0.0) else availablePercents.getOrNull(selectedPercentIndex)?.let(::calculateTipByPercent) ?: 0.0
    val totalAmount: Double get() = billAmount + selectedTipAmount
    val isPayEnabled: Boolean get() = !isLoading && totalAmount > 0
}

class TipSelectionViewModel(
    private val billAmount: Double,
    private val getTransactionRangeUseCase: GetTransactionRangeUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TipSelectionUiState(billAmount = billAmount))
    val uiState: StateFlow<TipSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeProfileUseCase().first()?.let { p ->
                _uiState.update { it.copy(waiterName = listOf(p.firstName, p.lastName).filter { s -> !s.isNullOrBlank() }.joinToString(" ").ifBlank { "Ваш официант" }, waiterStatus = p.status.ifBlank { "Коплю на отпуск!" }) }
            }
            getTransactionRangeUseCase().onSuccess { range ->
                val percents = range.percents
                val idx = if (percents.isEmpty()) 0 else range.defaultIndex.coerceIn(0, percents.lastIndex)
                _uiState.update { it.copy(isLoading = false, availablePercents = percents, selectedPercentIndex = idx) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Не удалось загрузить диапазон чаевых") }
            }
        }
    }

    fun selectPreset(index: Int) { _uiState.update { it.copy(selectedPercentIndex = index, isCustomSelected = false, customTipAmount = null) } }
    fun openCustomDialog() { _uiState.update { it.copy(showCustomTipDialog = true) } }
    fun dismissCustomDialog() { _uiState.update { it.copy(showCustomTipDialog = false) } }
    fun applyCustom(amount: String) { _uiState.update { it.copy(customTipAmount = amount.toDoubleOrNull() ?: 0.0, isCustomSelected = true, showCustomTipDialog = false) } }
    fun onMessageShown() { _uiState.update { it.copy(errorMessage = null) } }
}

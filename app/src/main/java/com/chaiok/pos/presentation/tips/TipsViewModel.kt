package com.chaiok.pos.presentation.tips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.TipRecord
import com.chaiok.pos.domain.usecase.GetTipsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TipsSummary(
    val todayAmount: Double = 0.0,
    val count: Int = 0,
    val avgPercent: Double = 0.0
)

data class TipsUiState(
    val isLoading: Boolean = false,
    val tips: List<TipRecord> = emptyList(),
    val summary: TipsSummary = TipsSummary(),
    val error: String? = null
)

class TipsViewModel(
    private val getTipsUseCase: GetTipsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TipsUiState(isLoading = true))
    val uiState: StateFlow<TipsUiState> = _uiState.asStateFlow()

    init {
        loadTips()
    }

    private fun loadTips() {
        viewModelScope.launch {
            _uiState.value = TipsUiState(isLoading = true)
            val result = getTipsUseCase()
            result.onSuccess { tips ->
                val today = LocalDate.now()
                val todayTips = tips.filter { it.dateTime.toLocalDate() == today }
                val summary = TipsSummary(
                    todayAmount = todayTips.sumOf { it.tipAmount },
                    count = tips.size,
                    avgPercent = if (tips.isEmpty()) 0.0 else tips.map { it.tipPercent }.average()
                )
                _uiState.value = TipsUiState(tips = tips, summary = summary)
            }.onFailure {
                _uiState.value = TipsUiState(error = "Не удалось загрузить историю чаевых")
            }
        }
    }
}

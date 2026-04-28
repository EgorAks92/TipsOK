package com.chaiok.pos.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.usecase.ObserveCurrentStatusUseCase
import com.chaiok.pos.domain.usecase.UpdateStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatusUiState(
    val selectedStatus: String = "",
    val isSaving: Boolean = false,
    val successMessage: String? = null
)

class StatusViewModel(
    private val updateStatusUseCase: UpdateStatusUseCase,
    observeCurrentStatusUseCase: ObserveCurrentStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private var isManualEdit = false

    init {
        viewModelScope.launch {
            observeCurrentStatusUseCase().collect { currentStatus ->
                if (!isManualEdit && currentStatus.isNotBlank()) {
                    _uiState.update { it.copy(selectedStatus = currentStatus) }
                }
            }
        }
    }

    fun onStatusChanged(value: String) {
        isManualEdit = true
        _uiState.update { it.copy(selectedStatus = value, successMessage = null) }
    }

    fun saveStatus() {
        if (_uiState.value.selectedStatus.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateStatusUseCase(_uiState.value.selectedStatus)
            _uiState.update { it.copy(isSaving = false, successMessage = "Статус сохранен") }
            isManualEdit = false
        }
    }
}

package com.chaiok.pos.presentation.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.UpdateIntegrationModeUseCase
import com.chaiok.pos.domain.usecase.UpdateTableModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IntegrationUiState(
    val settings: AppSettings = AppSettings(false, false, "default")
)

class IntegrationViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateIntegrationModeUseCase: UpdateIntegrationModeUseCase,
    private val updateTableModeUseCase: UpdateTableModeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntegrationUiState())
    val uiState: StateFlow<IntegrationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun toggleIntegration(enabled: Boolean) {
        viewModelScope.launch { updateIntegrationModeUseCase(enabled) }
    }

    fun toggleTableMode(enabled: Boolean) {
        viewModelScope.launch { updateTableModeUseCase(enabled) }
    }
}

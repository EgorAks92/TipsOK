package com.chaiok.pos.presentation.background

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.UpdateTileBackgroundUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileBackgroundUiState(
    val selectedBackground: String = "default"
)

class ProfileBackgroundViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateTileBackgroundUseCase: UpdateTileBackgroundUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileBackgroundUiState())
    val uiState: StateFlow<ProfileBackgroundUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(selectedBackground = settings.selectedTileBackground) }
            }
        }
    }

    fun setBackground(background: String) {
        viewModelScope.launch { updateTileBackgroundUseCase(background) }
    }
}

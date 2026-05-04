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
    val selectedBackground: String? = WaiterBackgroundMemoryCache.currentBackground
)

class ProfileBackgroundViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateTileBackgroundUseCase: UpdateTileBackgroundUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileBackgroundUiState(
            selectedBackground = WaiterBackgroundMemoryCache.currentBackground
        )
    )
    val uiState: StateFlow<ProfileBackgroundUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                val background = settings.tileBackground.ifBlank { DEFAULT_BACKGROUND }

                WaiterBackgroundMemoryCache.setCurrentBackground(background)

                _uiState.update {
                    it.copy(selectedBackground = background)
                }
            }
        }
    }

    fun setBackground(background: String) {
        val normalizedBackground = background.ifBlank { DEFAULT_BACKGROUND }

        WaiterBackgroundMemoryCache.setCurrentBackground(normalizedBackground)

        _uiState.update {
            it.copy(selectedBackground = normalizedBackground)
        }

        viewModelScope.launch {
            updateTileBackgroundUseCase(normalizedBackground)
        }
    }

    private companion object {
        private const val DEFAULT_BACKGROUND = "default"
    }
}
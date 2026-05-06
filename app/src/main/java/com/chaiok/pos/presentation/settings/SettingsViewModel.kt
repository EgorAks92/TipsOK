package com.chaiok.pos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.UpdatePcUsbModeUseCase
import com.chaiok.pos.presentation.background.WaiterBackgroundMemoryCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val waiterName: String = "Ваш официант",
    val waiterStatus: String = "Коплю на отпуск!",
    val tileBackground: String? = WaiterBackgroundMemoryCache.currentBackground,
    val pcUsbModeEnabled: Boolean = false
)

class SettingsViewModel(
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val updatePcUsbModeUseCase: UpdatePcUsbModeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            tileBackground = WaiterBackgroundMemoryCache.currentBackground
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                observeProfileUseCase(),
                observeSettingsUseCase()
            ) { profile, settings ->
                val firstName = profile?.firstName.orEmpty().trim()
                val lastName = profile?.lastName.orEmpty().trim()

                val waiterName = listOf(firstName, lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Ваш официант" }

                val waiterStatus = profile?.status
                    ?.ifBlank { "Коплю на отпуск!" }
                    ?: "Коплю на отпуск!"

                val background = settings.tileBackground.ifBlank { "default" }

                WaiterBackgroundMemoryCache.setCurrentBackground(background)

                SettingsUiState(
                    waiterName = waiterName,
                    waiterStatus = waiterStatus,
                    tileBackground = background,
                    pcUsbModeEnabled = settings.pcUsbModeEnabled
                )
            }.collect { nextState ->
                _uiState.update { nextState }
            }
        }
    }

    fun togglePcUsbMode(enabled: Boolean) {
        viewModelScope.launch {
            updatePcUsbModeUseCase(enabled)
        }
    }
}
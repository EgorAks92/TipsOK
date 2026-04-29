package com.chaiok.pos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val waiterName: String = "Ваш официант",
    val waiterStatus: String = "Коплю на отпуск!"
)

class SettingsViewModel(
    private val observeProfileUseCase: ObserveProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            observeProfileUseCase().collect { profile ->
                val firstName = profile?.firstName.orEmpty().trim()
                val lastName = profile?.lastName.orEmpty().trim()

                val waiterName = listOf(firstName, lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Ваш официант" }

                val waiterStatus = profile?.status
                    ?.ifBlank { "Коплю на отпуск!" }
                    ?: "Коплю на отпуск!"

                _uiState.update {
                    it.copy(
                        waiterName = waiterName,
                        waiterStatus = waiterStatus
                    )
                }
            }
        }
    }
}
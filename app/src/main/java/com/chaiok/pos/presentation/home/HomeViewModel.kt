package com.chaiok.pos.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.usecase.LogoutUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val profile: WaiterProfile? = null,
    val settings: AppSettings = AppSettings(false, false, "default"),
    val amountInput: String = "",
    val snackbarMessage: String? = null,
    val showLinkCardDialog: Boolean = false
)

sealed interface HomeEvent {
    data object NavigateToLogin : HomeEvent
}

class HomeViewModel(
    observeProfileUseCase: ObserveProfileUseCase,
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var linkCardDialogHandledInSession = false

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val oneTimeEvents = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(observeProfileUseCase(), observeSettingsUseCase()) { profile, settings ->
                profile to settings
            }.collect { (profile, settings) ->
                val shouldShowDialog = profile?.hasLinkedCard == false && !linkCardDialogHandledInSession
                _uiState.update {
                    it.copy(
                        profile = profile,
                        settings = settings,
                        showLinkCardDialog = shouldShowDialog
                    )
                }
            }
        }
    }

    fun onAmountDigitPressed(digit: String) {
        _uiState.update {
            if (it.amountInput.length >= 6) it else it.copy(amountInput = it.amountInput + digit)
        }
    }

    fun onAmountDeletePressed() {
        _uiState.update { it.copy(amountInput = it.amountInput.dropLast(1)) }
    }

    fun onConfirmAmount() {
        _uiState.update { it.copy(snackbarMessage = "Сумма принята. Интеграция будет подключена позже.") }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun dismissLinkCardDialog() {
        linkCardDialogHandledInSession = true
        _uiState.update { it.copy(showLinkCardDialog = false) }
    }

    fun onCardBindingStarted() {
        linkCardDialogHandledInSession = true
        _uiState.update { it.copy(showLinkCardDialog = false) }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            events.send(HomeEvent.NavigateToLogin)
        }
    }
}

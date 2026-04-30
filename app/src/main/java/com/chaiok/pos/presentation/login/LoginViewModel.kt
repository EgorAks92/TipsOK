package com.chaiok.pos.presentation.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.LoginWithPinUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val pin: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val triggerShake: Int = 0
)

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
}

class LoginViewModel(
    private val loginWithPinUseCase: LoginWithPinUseCase,
    private val getTransactionRangeUseCase: GetTransactionRangeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val events = Channel<LoginEvent>(Channel.BUFFERED)
    val oneTimeEvents = events.receiveAsFlow()

    fun onDigitPressed(digit: String) {
        _uiState.update {
            if (it.pin.length >= 4) it else it.copy(pin = it.pin + digit, errorMessage = null)
        }
    }

    fun onDeletePressed() {
        _uiState.update {
            if (it.pin.isEmpty()) it else it.copy(pin = it.pin.dropLast(1), errorMessage = null)
        }
    }

    fun onLoginPressed() {
        val state = _uiState.value
        if (state.isLoading || state.pin.length < 4) {
            Log.e("LoginFlow", "onLoginPressed ignored: loading=${state.isLoading} pinLength=${state.pin.length}")
            return
        }
        Log.e("LoginFlow", "onLoginPressed called pinLength=${state.pin.length}")
        val pin = state.pin
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = loginWithPinUseCase(pin)
            result.onSuccess {
                Log.e("LoginFlow", "login usecase success")
                viewModelScope.launch {
                    getTransactionRangeUseCase.refresh()
                        .onFailure { refreshError ->
                            Log.e("LoginFlow", "refreshTransactionRange failed after login", refreshError)
                        }
                }
                _uiState.update { state -> state.copy(isLoading = false, errorMessage = null, pin = "") }
                events.send(LoginEvent.NavigateToHome)
            }.onFailure { throwable ->
                Log.e("LoginFlow", "login usecase failed: ${throwable.message}", throwable)
                val message = (throwable as? DomainError)?.message ?: DomainError.InvalidPin.message
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = message,
                        pin = "",
                        triggerShake = state.triggerShake + 1
                    )
                }
            }
        }
    }
}

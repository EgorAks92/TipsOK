package com.chaiok.pos.presentation.cardbinding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.usecase.LinkCardUseCase
import com.chaiok.pos.domain.usecase.ReadCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CardBindingStatus { Idle, Reading, Success, Error }

data class CardBindingUiState(
    val status: CardBindingStatus = CardBindingStatus.Idle,
    val message: String = "Приложите карту к POS-терминалу для регистрации."
)

class CardBindingViewModel(
    private val readCardUseCase: ReadCardUseCase,
    private val linkCardUseCase: LinkCardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardBindingUiState())
    val uiState: StateFlow<CardBindingUiState> = _uiState.asStateFlow()

    fun readCard() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = CardBindingStatus.Reading,
                    message = "Идет чтение карты..."
                )
            }

            val cardData = readCardUseCase().getOrElse { throwable ->
                val message = (throwable as? DomainError)?.message
                    ?: DomainError.CardReadFailed.message
                    ?: "Не удалось прочитать карту. Попробуйте еще раз."

                _uiState.update { state ->
                    state.copy(
                        status = CardBindingStatus.Error,
                        message = message
                    )
                }
                return@launch
            }

            linkCardUseCase(cardData.cardSha256, cardData.cardToken)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            status = CardBindingStatus.Success,
                            message = "Карта успешно прочитана и привязана."
                        )
                    }
                }
                .onFailure { throwable ->
                    val message = (throwable as? DomainError)?.message
                        ?: DomainError.CardLinkFailed.message
                        ?: "Не удалось сохранить привязку карты. Попробуйте еще раз."

                    _uiState.update { state ->
                        state.copy(
                            status = CardBindingStatus.Error,
                            message = message
                        )
                    }
                }
        }
    }
}
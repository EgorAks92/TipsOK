package com.chaiok.pos.presentation.cardbinding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.data.service.CardReaderService
import com.chaiok.pos.domain.model.AppError
import com.chaiok.pos.domain.usecase.LinkCardUseCase
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
    private val cardReaderService: CardReaderService,
    private val linkCardUseCase: LinkCardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardBindingUiState())
    val uiState: StateFlow<CardBindingUiState> = _uiState.asStateFlow()

    fun readCard() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = CardBindingStatus.Reading, message = "Идет чтение карты...") }
            val readResult = cardReaderService.readCard()
            readResult.onSuccess { card ->
                linkCardUseCase(card.cardSha256, card.encryptedCardToken)
                _uiState.update {
                    it.copy(
                        status = CardBindingStatus.Success,
                        message = "Карта успешно прочитана и привязана."
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(status = CardBindingStatus.Error, message = AppError.CardReadError.message)
                }
            }
        }
    }
}

package com.chaiok.pos.presentation.cardpresenting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaiok.pos.domain.model.PaymentResult
import com.chaiok.pos.domain.model.PosPaymentEvent
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.domain.model.toPaymentResultOrNull
import com.chaiok.pos.domain.usecase.CancelPosPaymentUseCase
import com.chaiok.pos.domain.usecase.StartPosPaymentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CardPresentingStage {
    Idle,
    Preparing,
    WaitingForCard,
    CardDetected,
    Processing,
    PinRequired,
    Approved,
    Declined,
    Error,
    Cancelling,
    Cancelled
}

data class CardPresentingUiState(
    val stage: CardPresentingStage = CardPresentingStage.Idle,
    val title: String = "Поднесите карту",
    val message: String = "Приложите карту, телефон или часы к терминалу",
    val amountText: String = "",
    val canCancel: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CardPresentingOneTimeEvent {
    data class PaymentFinished(
        val result: PaymentResult
    ) : CardPresentingOneTimeEvent

    data object Cancelled : CardPresentingOneTimeEvent
}

class CardPresentingViewModel(
    private val startPosPaymentUseCase: StartPosPaymentUseCase,
    private val cancelPosPaymentUseCase: CancelPosPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardPresentingUiState())
    val uiState: StateFlow<CardPresentingUiState> = _uiState.asStateFlow()

    private val events = Channel<CardPresentingOneTimeEvent>(Channel.BUFFERED)
    val oneTimeEvents = events.receiveAsFlow()

    private var paymentJob: Job? = null
    private var paymentStarted = false
    private var terminalEventDelivered = false

    fun startPayment(
        request: PosPaymentRequest,
        amountText: String
    ) {
        if (paymentStarted) return

        paymentStarted = true
        terminalEventDelivered = false

        Log.i(
            PAYMENT_TAG,
            "CardPresenting startPayment amountText=$amountText terminalId=***${request.terminalId.takeLast(4)}"
        )

        _uiState.update {
            CardPresentingUiState(
                stage = CardPresentingStage.Preparing,
                title = "Подготовка оплаты",
                message = "Подключаемся к платежному сервису",
                amountText = amountText,
                canCancel = true,
                isLoading = true
            )
        }

        paymentJob?.cancel()
        paymentJob = viewModelScope.launch {
            startPosPaymentUseCase(request).collect { paymentEvent ->
                handlePaymentEvent(paymentEvent)
            }
        }
    }

    fun cancelPayment() {
        val currentStage = _uiState.value.stage

        if (!currentStage.canRequestCancel()) {
            Log.i(
                PAYMENT_TAG,
                "CardPresenting cancel ignored stage=$currentStage"
            )
            return
        }

        Log.i(
            PAYMENT_TAG,
            "CardPresenting cancel requested stage=$currentStage"
        )

        _uiState.update {
            it.copy(
                stage = CardPresentingStage.Cancelling,
                title = "Отменяем оплату",
                message = "Пожалуйста, подождите",
                canCancel = false,
                isLoading = true
            )
        }

        viewModelScope.launch {
            cancelPosPaymentUseCase()
        }
    }

    private suspend fun handlePaymentEvent(event: PosPaymentEvent) {
        Log.i(
            PAYMENT_TAG,
            "CardPresenting handlePaymentEvent event=${event.javaClass.simpleName}"
        )

        when (event) {
            PosPaymentEvent.Preparing -> {
                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.Preparing,
                        title = "Подготовка оплаты",
                        message = "Подключаемся к платежному сервису",
                        canCancel = true,
                        isLoading = true,
                        errorMessage = null
                    )
                }
            }

            PosPaymentEvent.WaitingForCard -> {
                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.WaitingForCard,
                        title = "Поднесите карту",
                        message = "Приложите карту, телефон или часы к терминалу",
                        canCancel = true,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }

            PosPaymentEvent.CardDetected -> {
                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.CardDetected,
                        title = "Карта считана",
                        message = "Не убирайте карту, операция продолжается",
                        canCancel = false,
                        isLoading = true,
                        errorMessage = null
                    )
                }
            }

            PosPaymentEvent.Processing -> {
                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.Processing,
                        title = "Обработка оплаты",
                        message = "Связываемся с банком",
                        canCancel = false,
                        isLoading = true,
                        errorMessage = null
                    )
                }
            }

            PosPaymentEvent.PinRequired -> {
                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.PinRequired,
                        title = "Введите PIN",
                        message = "Введите PIN-код на терминале и подтвердите операцию",
                        canCancel = false,
                        isLoading = true,
                        errorMessage = null
                    )
                }
            }

            is PosPaymentEvent.Approved -> {
                Log.i(
                    PAYMENT_TAG,
                    "CardPresenting approved event messagePreview=${event.message.toPaymentMessagePreview()}"
                )

                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.Approved,
                        title = "Оплата одобрена",
                        message = event.message ?: "Операция успешно завершена",
                        canCancel = false,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                deliverTerminalEvent(event, APPROVED_RESULT_HOLD_MS)
            }

            is PosPaymentEvent.Declined -> {
                Log.i(
                    PAYMENT_TAG,
                    "CardPresenting declined event reasonPreview=${event.reason.toPaymentMessagePreview()}"
                )

                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.Declined,
                        title = "Оплата отклонена",
                        message = event.reason ?: "Операция отклонена банком",
                        canCancel = false,
                        isLoading = false,
                        errorMessage = event.reason
                    )
                }

                deliverTerminalEvent(event, FAILED_RESULT_HOLD_MS)
            }

            is PosPaymentEvent.Error -> {
                Log.i(
                    PAYMENT_TAG,
                    "CardPresenting error event messagePreview=${event.message.toPaymentMessagePreview()}"
                )

                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.Error,
                        title = "Ошибка оплаты",
                        message = event.message,
                        canCancel = false,
                        isLoading = false,
                        errorMessage = event.message
                    )
                }

                deliverTerminalEvent(event, FAILED_RESULT_HOLD_MS)
            }

            PosPaymentEvent.Cancelled -> {
                Log.i(
                    PAYMENT_TAG,
                    "CardPresenting cancelled event"
                )

                _uiState.update {
                    it.copy(
                        stage = CardPresentingStage.Cancelled,
                        title = "Оплата отменена",
                        message = "Операция была отменена",
                        canCancel = false,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                events.send(CardPresentingOneTimeEvent.Cancelled)
            }
        }
    }

    private suspend fun deliverTerminalEvent(
        event: PosPaymentEvent,
        holdDurationMs: Long
    ) {
        if (terminalEventDelivered) {
            Log.i(
                PAYMENT_TAG,
                "CardPresenting terminal event ignored because already delivered"
            )
            return
        }

        val result = event.toPaymentResultOrNull()

        Log.i(
            PAYMENT_TAG,
            "CardPresenting deliverTerminalEvent result=${result?.javaClass?.simpleName ?: "null"}"
        )

        if (result == null) return

        terminalEventDelivered = true
        if (holdDurationMs > 0) {
            delay(holdDurationMs)
        }
        events.send(
            CardPresentingOneTimeEvent.PaymentFinished(
                result = result
            )
        )
    }

    override fun onCleared() {
        super.onCleared()

        val shouldCancel = _uiState.value.stage.canRequestCancel()

        Log.i(
            PAYMENT_TAG,
            "CardPresenting onCleared shouldCancel=$shouldCancel stage=${_uiState.value.stage}"
        )

        if (shouldCancel) {
            viewModelScope.launch {
                cancelPosPaymentUseCase()
            }
        }

        paymentJob?.cancel()
    }
}

private fun CardPresentingStage.canRequestCancel(): Boolean {
    return when (this) {
        CardPresentingStage.Idle,
        CardPresentingStage.Preparing,
        CardPresentingStage.WaitingForCard -> true

        CardPresentingStage.CardDetected,
        CardPresentingStage.Processing,
        CardPresentingStage.PinRequired,
        CardPresentingStage.Approved,
        CardPresentingStage.Declined,
        CardPresentingStage.Error,
        CardPresentingStage.Cancelling,
        CardPresentingStage.Cancelled -> false
    }
}

private fun String?.toPaymentMessagePreview(): String {
    val normalized = this
        ?.replace("\n", " ")
        ?.replace("\r", " ")
        ?.trim()
        ?.take(160)

    return if (normalized.isNullOrBlank()) {
        "<blank>"
    } else {
        "\"$normalized\""
    }
}

private const val PAYMENT_TAG = "TipsPaymentFlow"
private const val APPROVED_RESULT_HOLD_MS = 3_000L
private const val FAILED_RESULT_HOLD_MS = 4_000L
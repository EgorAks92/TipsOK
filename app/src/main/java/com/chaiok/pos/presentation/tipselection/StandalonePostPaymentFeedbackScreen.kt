package com.chaiok.pos.presentation.tipselection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.presentation.pc.PaymentThankYouScreen
import com.chaiok.pos.presentation.pc.PcPostPaymentFeedbackScreen
import kotlinx.coroutines.delay

internal suspend fun countDownPostPaymentReview(secondsLeft: Int, onTick: (Int) -> Unit) {
    for (remaining in secondsLeft - 1 downTo 0) {
        delay(1_000)
        onTick(remaining)
    }
}

@Composable
internal fun StandalonePostPaymentFeedbackScreen(
    state: TipSelectionUiState,
    onKitchenEvaluation: (Int) -> Unit,
    onServiceEvaluation: (Int) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    var secondsLeft by rememberSaveable { mutableIntStateOf(15) }
    val bothSelected = state.serviceEvaluation in 1..5 && state.kitchenEvaluation in 1..5

    BackHandler {
        if (!bothSelected) onSkip()
    }

    LaunchedEffect(bothSelected) {
        if (bothSelected) {
            delay(2_000)
            onSubmit()
        } else {
            countDownPostPaymentReview(secondsLeft) { secondsLeft = it }
            onSkip()
        }
    }

    if (bothSelected) {
        PaymentThankYouScreen()
    } else {
        PcPostPaymentFeedbackScreen(
            designStyle = PcCompactPaymentDesignStyle.DEFAULT,
            serviceRating = state.serviceEvaluation.takeIf { it in 1..5 },
            kitchenRating = state.kitchenEvaluation.takeIf { it in 1..5 },
            secondsLeft = secondsLeft,
            onServiceRatingSelected = onServiceEvaluation,
            onKitchenRatingSelected = onKitchenEvaluation,
            onClose = onSkip,
            modifier = Modifier.fillMaxSize(),
            lightBackground = true
        )
    }
}

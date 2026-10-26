package com.chaiok.pos.presentation.cardpresenting

import androidx.compose.runtime.Composable
import com.chaiok.pos.presentation.pc.StandaloneCardPresentingContent

@Composable
fun CardPresentingScreen(
    state: CardPresentingUiState,
    onCancel: () -> Unit
) {
    StandaloneCardPresentingContent(state = state, onCancel = onCancel)
}

package com.chaiok.pos.presentation.cardbinding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar


fun CardBindingScreen(
    state: CardBindingUiState,
    onBack: () -> Unit,
    onReadCard: () -> Unit
) {
    val pulseScale = animateFloatAsState(
        targetValue = when (state.status) {
            CardBindingStatus.Success, CardBindingStatus.Error -> 1.25f
            else -> 1f
        },
        animationSpec = tween(420),
        label = "card_binding_state_scale"
    )

    Scaffold(topBar = { TiplyBackTopAppBar(title = "Привязка карты", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Text(state.message, style = MaterialTheme.typography.titleMedium)

            if (state.status == CardBindingStatus.Reading) {
                CircularProgressIndicator(modifier = Modifier.size(84.dp), strokeWidth = 6.dp)
            }

            AnimatedVisibility(state.status == CardBindingStatus.Success) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF24A148),
                    modifier = Modifier
                        .size(128.dp)
                        .graphicsLayer(scaleX = pulseScale.value, scaleY = pulseScale.value)
                )
            }
            AnimatedVisibility(state.status == CardBindingStatus.Error) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(128.dp)
                        .graphicsLayer(scaleX = pulseScale.value, scaleY = pulseScale.value)
                )
            }

            Button(onClick = onReadCard, modifier = Modifier.fillMaxWidth(), enabled = state.status != CardBindingStatus.Reading) {
                Text(if (state.status == CardBindingStatus.Reading) "Чтение..." else "Считать карту")
            }
        }
    }
}



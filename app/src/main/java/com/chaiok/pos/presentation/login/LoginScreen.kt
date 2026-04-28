package com.chaiok.pos.presentation.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaiok.pos.presentation.components.NumericKeypad

@Composable
fun LoginScreen(
    state: LoginUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onLogin: () -> Unit
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.triggerShake) {
        if (state.triggerShake > 0) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    -12f at 60
                    12f at 120
                    -10f at 180
                    10f at 240
                    0f at 350
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ЧайОК", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value }) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Вход по PIN", style = MaterialTheme.typography.titleLarge)
                Text("*".repeat(state.pin.length).padEnd(4, '•'), style = MaterialTheme.typography.headlineMedium)
                state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth(), enabled = state.pin.length == 4 && !state.isLoading) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Войти")
                    }
                }
            }
        }
        NumericKeypad(onDigit = onDigit, onBackspace = onDelete, onOk = onLogin, modifier = Modifier.fillMaxWidth())
    }
}

package com.chaiok.pos.presentation.cardbinding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CardBindingScreen(
    state: CardBindingUiState,
    onBack: () -> Unit,
    onReadCard: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Привязка карты") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
        })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(state.message, style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(state.status == CardBindingStatus.Success) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF24A148))
            }
            AnimatedVisibility(state.status == CardBindingStatus.Error) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onReadCard,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status != CardBindingStatus.Reading
            ) {
                Text("Считать карту")
            }
        }
    }
}

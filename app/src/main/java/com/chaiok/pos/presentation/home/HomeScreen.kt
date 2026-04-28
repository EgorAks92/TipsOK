package com.chaiok.pos.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chaiok.pos.presentation.components.NumericKeypad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onSnackbarShown: () -> Unit,
    onBindCard: () -> Unit,
    onDismissBindDialog: () -> Unit
) {
    val snackState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackState.showSnackbar(it)
            onSnackbarShown()
        }
    }
    if (state.showLinkCardDialog) {
        AlertDialog(
            onDismissRequest = onDismissBindDialog,
            confirmButton = { Button(onClick = onBindCard) { Text("Привязать карту") } },
            dismissButton = { Button(onClick = onDismissBindDialog) { Text("Позже") } },
            title = { Text("Карта не привязана") },
            text = { Text("К вашему профилю не привязана банковская карта. Привяжите карту, чтобы получать чаевые.") }
        )
    }
    Scaffold(snackbarHost = { SnackbarHost(hostState = snackState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF2066E2), Color(0xFF36CFC9))))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }
                    }
                    Text(
                        text = "${state.profile?.firstName.orEmpty()} ${state.profile?.lastName.orEmpty()}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(state.profile?.status ?: "", color = Color.White)
                }
            }

            if (state.settings.tableModeEnabled) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Режим столиков будет настроен позже",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Сумма счета", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (state.amountInput.isBlank()) "0 ₽" else "${state.amountInput} ₽",
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                }
                NumericKeypad(onDigit = onDigit, onBackspace = onBackspace)
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Подтвердить сумму") }
            }
        }
    }
}

package com.chaiok.pos.presentation.integration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar

@Composable
fun IntegrationScreen(
    state: IntegrationUiState,
    onBack: () -> Unit,
    onToggleIntegration: (Boolean) -> Unit,
    onToggleTableMode: (Boolean) -> Unit
) {
    Scaffold(topBar = { TiplyBackTopAppBar(title = "Интеграционный режим", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Интеграционный режим")
                    Text("Интеграционный режим будет использоваться для подключения к внешней POS/кассовой системе.")
                    Switch(checked = state.settings.integrationModeEnabled, onCheckedChange = onToggleIntegration)
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Режим столиков")
                    Text("При включении на главном экране будет отображаться режим работы со столиками.")
                    Switch(checked = state.settings.tableModeEnabled, onCheckedChange = onToggleTableMode)
                }
            }
        }
    }
}

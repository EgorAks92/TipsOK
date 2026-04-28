package com.chaiok.pos.presentation.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val predefined = listOf("На смене", "Перерыв", "Занят", "Готов принимать гостей")

@Composable
fun StatusScreen(
    state: StatusUiState,
    onBack: () -> Unit,
    onStatusChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Выбор статуса") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.selectedStatus,
                onValueChange = onStatusChanged,
                label = { Text("Новый статус") },
                modifier = Modifier.fillMaxWidth()
            )
            predefined.forEach { status ->
                Button(onClick = { onStatusChanged(status) }, modifier = Modifier.fillMaxWidth()) { Text(status) }
            }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Сохранить статус") }
            state.successMessage?.let { Text(it) }
        }
    }
}

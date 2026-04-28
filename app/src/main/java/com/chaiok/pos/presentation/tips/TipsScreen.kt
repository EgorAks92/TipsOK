package com.chaiok.pos.presentation.tips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(state: TipsUiState, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Мои чаевые") }, navigationIcon = {
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("За сегодня: ${"%.2f".format(state.summary.todayAmount)} ₽")
                    Text("Количество чаевых: ${state.summary.count}")
                    Text("Средний процент: ${"%.1f".format(state.summary.avgPercent)}%")
                }
            }
            if (state.tips.isEmpty()) {
                Text(if (state.isLoading) "Загрузка..." else "Пока нет записей")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.tips) { tip ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(tip.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                                Text("Счет: ${"%.2f".format(tip.billAmount)} ₽")
                                Text("Процент: ${tip.tipPercent}%")
                                Text("Чаевые: ${"%.2f".format(tip.tipAmount)} ₽")
                            }
                        }
                    }
                }
            }
        }
    }
}

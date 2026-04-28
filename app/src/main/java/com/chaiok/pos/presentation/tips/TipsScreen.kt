package com.chaiok.pos.presentation.tips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF2066E2), Color(0xFF36CFC9))))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryColumn("Сегодня", "${"%.2f".format(state.summary.todayAmount)} ₽")
                    SummaryColumn("Записей", state.summary.count.toString())
                    SummaryColumn("Средний %", "${"%.1f".format(state.summary.avgPercent)}%")
                }
            }
            if (state.tips.isEmpty()) {
                Text(if (state.isLoading) "Загрузка..." else "Пока нет записей", style = MaterialTheme.typography.titleMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.tips) { tip ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tip.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), style = MaterialTheme.typography.titleMedium)
                                Text("Счет: ${"%.2f".format(tip.billAmount)} ₽")
                                Text("Чаевые: ${"%.2f".format(tip.tipAmount)} ₽", color = MaterialTheme.colorScheme.primary)
                                Text("Процент: ${tip.tipPercent}%")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

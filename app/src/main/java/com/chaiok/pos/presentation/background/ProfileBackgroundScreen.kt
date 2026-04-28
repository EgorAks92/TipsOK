package com.chaiok.pos.presentation.background

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBackgroundScreen(
    state: ProfileBackgroundUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf("default", "sunset", "forest")

    Scaffold(topBar = {
        TopAppBar(title = { Text("Фон профиля") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
        })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(option)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(previewBrush(option))
                        )
                        if (state.selectedBackground == option) {
                            Text("Выбрано")
                        }
                    }
                }
            }
        }
    }
}

private fun previewBrush(background: String): Brush = when (background) {
    "sunset" -> Brush.horizontalGradient(listOf(Color(0xFFE96443), Color(0xFF904E95)))
    "forest" -> Brush.horizontalGradient(listOf(Color(0xFF134E5E), Color(0xFF71B280)))
    else -> Brush.horizontalGradient(listOf(Color(0xFF2066E2), Color(0xFF36CFC9)))
}

package com.chaiok.pos.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onOk: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "OK")
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    Button(
                        onClick = {
                            when (label) {
                                "⌫" -> onBackspace()
                                "OK" -> onOk?.invoke()
                                else -> onDigit(label)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

package com.chaiok.pos.presentation.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.presentation.theme.MontserratFontFamily

@Composable
fun PcCommandIdleScreen(
    state: PcUsbConnectionStatus,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE6EAF0),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        Text(
            text = "Ожидание команды с кассы",
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 24.dp)
        )

        Text(
            text = "Подключите ПК по USB",
            fontFamily = MontserratFontFamily,
            color = Color(0xFF69707A),
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = state.toDisplayText(),
            fontFamily = MontserratFontFamily,
            color = state.toDisplayColor(),
            modifier = Modifier.padding(top = 12.dp)
        )

        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(
                text = "Настройки",
                fontFamily = MontserratFontFamily
            )
        }
    }
}

private fun PcUsbConnectionStatus.toDisplayText(): String {
    return when (this) {
        PcUsbConnectionStatus.Idle -> {
            "USB-режим готов"
        }

        PcUsbConnectionStatus.BindingService -> {
            "Подключение к сервису USB-кассы"
        }

        PcUsbConnectionStatus.ServiceBound -> {
            "Сервис USB-кассы подключён"
        }

        PcUsbConnectionStatus.OpeningPort -> {
            "Открытие USB-порта"
        }

        PcUsbConnectionStatus.ConnectingPort -> {
            "Подключение к USB-порту"
        }

        PcUsbConnectionStatus.Connected -> {
            "USB-порт подключён"
        }

        PcUsbConnectionStatus.WaitingForData -> {
            "Ожидание команды оплаты от кассы"
        }

        is PcUsbConnectionStatus.Error -> {
            "Ошибка USB: $message"
        }
    }
}

private fun PcUsbConnectionStatus.toDisplayColor(): Color {
    return when (this) {
        is PcUsbConnectionStatus.Error -> Color(0xFFD32F2F)
        else -> Color(0xFF69707A)
    }
}
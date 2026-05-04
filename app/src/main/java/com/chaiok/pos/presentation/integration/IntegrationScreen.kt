package com.chaiok.pos.presentation.integration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

@Composable
fun IntegrationScreen(
    state: IntegrationUiState,
    onBack: () -> Unit,
    onToggleIntegration: (Boolean) -> Unit,
    onToggleTableMode: (Boolean) -> Unit
) {
    Scaffold(topBar = { IntegrationTopAppBar(onBack = onBack) }) { padding ->
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

@Composable
private fun IntegrationTopAppBar(onBack: () -> Unit) {
    val barShape = RoundedCornerShape(bottomStart = 46.dp, bottomEnd = 46.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(22.dp, barShape, clip = false)
            .clip(barShape)
            .background(Color.White)
            .padding(start = 32.dp, end = 32.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Text(
            text = "Интеграционный режим",
            modifier = Modifier.align(Alignment.Center),
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings_back),
                contentDescription = "Назад",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color(0xFF1B2128))
            )
        }
    }
}

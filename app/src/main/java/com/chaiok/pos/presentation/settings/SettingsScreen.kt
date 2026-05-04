package com.chaiok.pos.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.components.WaiterProfileCardHeader
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val SettingsBackgroundColor = Color.White
private val SettingsPrimaryTextColor = Color(0xFF1B2128)
private val SettingsSecondaryTextColor = Color(0xFF69707A)
private val SettingsCardColor = Color(0xFFF7F8FA)
private val SettingsIconBackgroundColor = Color(0xFFF0F2F4)
private val SettingsAccentColor = Color(0xFF087BE8)

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onCardBinding: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onIntegration: () -> Unit,
    onBackground: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    SettingsScreen(
        state = state,
        onBack = onBack,
        onCardBinding = onCardBinding,
        onStatus = onStatus,
        onTips = onTips,
        onIntegration = onIntegration,
        onBackground = onBackground
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onCardBinding: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onIntegration: () -> Unit,
    onBackground: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                WaiterProfileCardHeader(
                    waiterName = state.waiterName,
                    waiterStatus = state.waiterStatus
                )

                TiplyBackTopAppBar(
                    title = "Настройки",
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsItem(
                    title = "Привязка карты",
                    subtitle = "Зарегистрировать карту для получения чаевых",
                    iconRes = R.drawable.ic_settings_card,
                    onClick = onCardBinding
                )

                SettingsItem(
                    title = "Выбор статуса",
                    subtitle = "Обновить рабочий статус",
                    iconRes = R.drawable.ic_settings_status,
                    onClick = onStatus
                )

                SettingsItem(
                    title = "Мои чаевые",
                    subtitle = "История и сводка по чаевым",
                    iconRes = R.drawable.ic_settings_tips,
                    onClick = onTips
                )

                SettingsItem(
                    title = "Интеграционный режим",
                    subtitle = "Настройка POS-интеграции",
                    iconRes = R.drawable.ic_settings_integration,
                    onClick = onIntegration
                )

                SettingsItem(
                    title = "Фон профиля",
                    subtitle = "Настройка плитки на главном экране",
                    iconRes = R.drawable.ic_settings_background,
                    onClick = onBackground
                )
            }
        }
    }
}


@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(SettingsCardColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsIconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(SettingsAccentColor)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 10.dp)
        ) {
            Text(
                text = title,
                color = SettingsPrimaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = SettingsSecondaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "›",
            color = SettingsPrimaryTextColor.copy(alpha = 0.42f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 28.sp
        )
    }
}

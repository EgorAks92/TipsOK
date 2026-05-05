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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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

private data class SettingsLayoutMetrics(
    val isSquareCompact: Boolean,
    val headerToContentSpacing: Dp,
    val horizontalPadding: Dp,
    val cardsSpacing: Dp,

    val itemCornerRadius: Dp,
    val itemShadowElevation: Dp,
    val itemHorizontalPadding: Dp,
    val itemVerticalPadding: Dp,

    val iconBoxSize: Dp,
    val iconBoxCornerRadius: Dp,
    val iconSize: Dp,

    val textStartPadding: Dp,
    val textEndPadding: Dp,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val titleSubtitleSpacing: Dp,
    val subtitleFontSize: TextUnit,
    val subtitleLineHeight: TextUnit,
    val subtitleMaxLines: Int,

    val arrowFontSize: TextUnit,
    val arrowLineHeight: TextUnit
)

@Composable
private fun settingsLayoutMetrics(): SettingsLayoutMetrics {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp

    return if (isSquareCompact) {
        SettingsLayoutMetrics(
            isSquareCompact = true,
            headerToContentSpacing = 10.dp,
            horizontalPadding = 14.dp,
            cardsSpacing = 8.dp,

            itemCornerRadius = 18.dp,
            itemShadowElevation = 3.dp,
            itemHorizontalPadding = 12.dp,
            itemVerticalPadding = 9.dp,

            iconBoxSize = 36.dp,
            iconBoxCornerRadius = 13.dp,
            iconSize = 20.dp,

            textStartPadding = 10.dp,
            textEndPadding = 6.dp,
            titleFontSize = 13.sp,
            titleLineHeight = 16.sp,
            titleSubtitleSpacing = 2.dp,
            subtitleFontSize = 11.sp,
            subtitleLineHeight = 14.sp,
            subtitleMaxLines = 1,

            arrowFontSize = 24.sp,
            arrowLineHeight = 24.sp
        )
    } else {
        SettingsLayoutMetrics(
            isSquareCompact = false,
            headerToContentSpacing = 22.dp,
            horizontalPadding = 24.dp,
            cardsSpacing = 12.dp,

            itemCornerRadius = 24.dp,
            itemShadowElevation = 5.dp,
            itemHorizontalPadding = 16.dp,
            itemVerticalPadding = 14.dp,

            iconBoxSize = 44.dp,
            iconBoxCornerRadius = 16.dp,
            iconSize = 24.dp,

            textStartPadding = 14.dp,
            textEndPadding = 10.dp,
            titleFontSize = 15.sp,
            titleLineHeight = 19.sp,
            titleSubtitleSpacing = 4.dp,
            subtitleFontSize = 12.sp,
            subtitleLineHeight = 16.sp,
            subtitleMaxLines = 2,

            arrowFontSize = 28.sp,
            arrowLineHeight = 28.sp
        )
    }
}

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onBackground: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    SettingsScreen(
        state = state,
        onBack = onBack,
        onStatus = onStatus,
        onTips = onTips,
        onBackground = onBackground
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onBackground: () -> Unit
) {
    val metrics = settingsLayoutMetrics()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                WaiterProfileCardHeader(
                    waiterName = state.waiterName,
                    waiterStatus = state.waiterStatus,
                    background = state.tileBackground
                )

                TiplyBackTopAppBar(
                    title = "Настройки",
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Spacer(modifier = Modifier.height(metrics.headerToContentSpacing))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = metrics.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(metrics.cardsSpacing)
            ) {
                SettingsItem(
                    title = "Выбор статуса",
                    subtitle = "Обновить рабочий статус",
                    iconRes = R.drawable.ic_settings_status,
                    metrics = metrics,
                    onClick = onStatus
                )

                SettingsItem(
                    title = "Мои чаевые",
                    subtitle = "История и сводка по чаевым",
                    iconRes = R.drawable.ic_settings_tips,
                    metrics = metrics,
                    onClick = onTips
                )

                SettingsItem(
                    title = "Фон профиля",
                    subtitle = "Настройка плитки на главном экране",
                    iconRes = R.drawable.ic_settings_background,
                    metrics = metrics,
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
    metrics: SettingsLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.itemShadowElevation,
                shape = RoundedCornerShape(metrics.itemCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(metrics.itemCornerRadius))
            .background(SettingsCardColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = metrics.itemHorizontalPadding,
                vertical = metrics.itemVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(metrics.iconBoxSize)
                .clip(RoundedCornerShape(metrics.iconBoxCornerRadius))
                .background(SettingsIconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(metrics.iconSize),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(SettingsAccentColor)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = metrics.textStartPadding,
                    end = metrics.textEndPadding
                )
        ) {
            Text(
                text = title,
                color = SettingsPrimaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.titleFontSize,
                lineHeight = metrics.titleLineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(metrics.titleSubtitleSpacing))

            Text(
                text = subtitle,
                color = SettingsSecondaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = metrics.subtitleFontSize,
                lineHeight = metrics.subtitleLineHeight,
                maxLines = metrics.subtitleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "›",
            color = SettingsPrimaryTextColor.copy(alpha = 0.42f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.arrowFontSize,
            lineHeight = metrics.arrowLineHeight
        )
    }
}
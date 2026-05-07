package com.chaiok.pos.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.components.WaiterProfileCardHeader
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val SettingsBackgroundColor = Color.White
private val SettingsPrimaryTextColor = Color(0xFF1B2128)
private val SettingsSecondaryTextColor = Color(0xFF69707A)
private val SettingsCardColor = Color(0xFFF7F8FA)
private val SettingsIconBackgroundColor = Color(0xFFF0F2F4)
private val SettingsAccentColor = Color(0xFF087BE8)
private val SettingsGreenColor = Color(0xFF14B8A6)
private val SettingsStrokeColor = Color(0xFFE2E7EF)

private data class SettingsPremiumMetrics(
    val contentHorizontalPadding: Dp,
    val contentTopPadding: Dp,
    val contentBottomPadding: Dp,
    val headerTitleFontSize: TextUnit,
    val headerTitleLineHeight: TextUnit,
    val headerSubtitleFontSize: TextUnit,
    val headerSubtitleLineHeight: TextUnit,
    val headerToCardsSpacing: Dp,
    val cardsSpacing: Dp,

    val itemHeight: Dp,
    val itemCornerRadius: Dp,
    val itemShadowElevation: Dp,
    val itemHorizontalPadding: Dp,
    val iconBoxSize: Dp,
    val iconBoxCornerRadius: Dp,
    val iconSize: Dp,
    val textStartPadding: Dp,
    val textEndPadding: Dp,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val subtitleFontSize: TextUnit,
    val subtitleLineHeight: TextUnit,
    val subtitleMaxLines: Int,
    val arrowBoxSize: Dp,
    val arrowFontSize: TextUnit,
    val arrowLineHeight: TextUnit
)

private fun squarePremiumSettingsMetrics(): SettingsPremiumMetrics {
    return SettingsPremiumMetrics(
        contentHorizontalPadding = 16.dp,
        contentTopPadding = 14.dp,
        contentBottomPadding = 12.dp,
        headerTitleFontSize = 19.sp,
        headerTitleLineHeight = 23.sp,
        headerSubtitleFontSize = 12.sp,
        headerSubtitleLineHeight = 16.sp,
        headerToCardsSpacing = 12.dp,
        cardsSpacing = 10.dp,

        itemHeight = 66.dp,
        itemCornerRadius = 22.dp,
        itemShadowElevation = 4.dp,
        itemHorizontalPadding = 12.dp,
        iconBoxSize = 40.dp,
        iconBoxCornerRadius = 15.dp,
        iconSize = 21.dp,
        textStartPadding = 12.dp,
        textEndPadding = 8.dp,
        titleFontSize = 14.sp,
        titleLineHeight = 17.sp,
        subtitleFontSize = 11.sp,
        subtitleLineHeight = 14.sp,
        subtitleMaxLines = 1,
        arrowBoxSize = 28.dp,
        arrowFontSize = 22.sp,
        arrowLineHeight = 22.sp
    )
}

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onBackground: () -> Unit,
    onPcIdleImages: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    SettingsScreen(
        state = state,
        onBack = onBack,
        onStatus = onStatus,
        onTips = onTips,
        onBackground = onBackground,
        onPcIdleImages = onPcIdleImages,
        onTogglePcUsbMode = viewModel::togglePcUsbMode
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onBackground: () -> Unit,
    onPcIdleImages: () -> Unit,
    onTogglePcUsbMode: (Boolean) -> Unit
) {
    when (rememberChaiOkDeviceClass()) {
        ChaiOkDeviceClass.SquareCompact -> {
            SettingsSquarePremiumScreen(
                state = state,
                onBack = onBack,
                onStatus = onStatus,
                onTips = onTips,
                onBackground = onBackground,
        onPcIdleImages = onPcIdleImages,
                onTogglePcUsbMode = onTogglePcUsbMode
            )
        }

        ChaiOkDeviceClass.Regular -> {
            SettingsRegularScreen(
                state = state,
                onBack = onBack,
                onStatus = onStatus,
                onTips = onTips,
                onBackground = onBackground,
        onPcIdleImages = onPcIdleImages,
                onTogglePcUsbMode = onTogglePcUsbMode
            )
        }
    }
}

@Composable
private fun SettingsRegularScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onBackground: () -> Unit,
    onPcIdleImages: () -> Unit,
    onTogglePcUsbMode: (Boolean) -> Unit
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
                    waiterStatus = state.waiterStatus,
                    background = state.tileBackground
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
                SettingsRegularItem(
                    title = "Выбор статуса",
                    subtitle = "Обновить рабочий статус",
                    iconRes = R.drawable.ic_settings_status,
                    onClick = onStatus
                )

                SettingsRegularItem(
                    title = "Мои чаевые",
                    subtitle = "История и сводка по чаевым",
                    iconRes = R.drawable.ic_settings_tips,
                    onClick = onTips
                )

                SettingsRegularItem(
                    title = "Фон профиля",
                    subtitle = "Настройка плитки на главном экране",
                    iconRes = R.drawable.ic_settings_background,
                    onClick = onBackground
                )

                SettingsRegularItem(
                    title = "Экран ожидания кассы",
                    subtitle = "Картинки в ECR-режиме",
                    iconRes = R.drawable.ic_settings_background,
                    onClick = onPcIdleImages
                )

                SettingsRegularToggleItem(
                    title = "Режим кассы по USB",
                    subtitle = if (state.pcUsbModeEnabled) "Приложение ждёт сумму от ПК по USB" else "Ожидание команды оплаты от ПК выключено",
                    iconRes = R.drawable.ic_settings_status,
                    checked = state.pcUsbModeEnabled,
                    onToggle = onTogglePcUsbMode
                )
            }
        }
    }
}

@Composable
private fun SettingsSquarePremiumScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onStatus: () -> Unit,
    onTips: () -> Unit,
    onBackground: () -> Unit,
    onPcIdleImages: () -> Unit,
    onTogglePcUsbMode: (Boolean) -> Unit
) {
    val metrics = squarePremiumSettingsMetrics()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TiplyBackTopAppBar(
                title = "Настройки",
                onBack = onBack,
                elevation = 10.dp,
                ambientAlpha = 0.16f,
                spotAlpha = 0.22f
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = metrics.contentHorizontalPadding)
                    .padding(
                        top = metrics.contentTopPadding,
                        bottom = metrics.contentBottomPadding
                    )
            ) {
                Spacer(modifier = Modifier.height(metrics.headerToCardsSpacing))

                Column(
                    verticalArrangement = Arrangement.spacedBy(metrics.cardsSpacing)
                ) {
                    SettingsPremiumItem(
                        title = "Статус",
                        subtitle = "Что видят гости",
                        iconRes = R.drawable.ic_settings_status,
                        metrics = metrics,
                        onClick = onStatus
                    )

                    SettingsPremiumItem(
                        title = "Чаевые",
                        subtitle = "История и статистика",
                        iconRes = R.drawable.ic_settings_tips,
                        metrics = metrics,
                        onClick = onTips
                    )

                    SettingsPremiumItem(
                        title = "Фон профиля",
                        subtitle = "Оформление карточки",
                        iconRes = R.drawable.ic_settings_background,
                        metrics = metrics,
                        onClick = onBackground
                    )

                    SettingsPremiumItem(
                        title = "Фон для кассового режима",
                        subtitle = "Слайд шоу для кассового режима",
                        iconRes = R.drawable.ic_cash,
                        metrics = metrics,
                        onClick = onPcIdleImages
                    )

                    SettingsPremiumToggleItem(
                        title = "Режим работы с кассой",
                        subtitle = if (state.pcUsbModeEnabled) "Включено" else "Выключено",
                        iconRes = R.drawable.ic_settings_pc_idle,
                        metrics = metrics,
                        checked = state.pcUsbModeEnabled,
                        onToggle = onTogglePcUsbMode
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRegularItem(
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

@Composable
private fun SettingsPremiumItem(
    title: String,
    subtitle: String,
    iconRes: Int,
    metrics: SettingsPremiumMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.itemHeight)
            .shadow(
                elevation = metrics.itemShadowElevation,
                shape = RoundedCornerShape(metrics.itemCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.055f),
                spotColor = Color.Black.copy(alpha = 0.105f)
            )
            .clip(RoundedCornerShape(metrics.itemCornerRadius))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = SettingsStrokeColor.copy(alpha = 0.86f),
                shape = RoundedCornerShape(metrics.itemCornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = metrics.itemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(metrics.iconBoxSize)
                .clip(RoundedCornerShape(metrics.iconBoxCornerRadius))
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            SettingsAccentColor.copy(alpha = 0.14f),
                            SettingsGreenColor.copy(alpha = 0.12f)
                        )
                    )
                ),
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

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = SettingsSecondaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.subtitleFontSize,
                lineHeight = metrics.subtitleLineHeight,
                maxLines = metrics.subtitleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(metrics.arrowBoxSize)
                .clip(RoundedCornerShape(metrics.arrowBoxSize / 2))
                .background(SettingsIconBackgroundColor.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "›",
                color = SettingsPrimaryTextColor.copy(alpha = 0.48f),
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.arrowFontSize,
                lineHeight = metrics.arrowLineHeight
            )
        }
    }
}

@Composable
private fun SettingsRegularToggleItem(title: String, subtitle: String, iconRes: Int, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SettingsCardColor).clickable { onToggle(!checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp), colorFilter = ColorFilter.tint(SettingsAccentColor))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontFamily = MontserratFontFamily, fontWeight = FontWeight.Bold, color = SettingsPrimaryTextColor)
            Text(subtitle, fontFamily = MontserratFontFamily, fontSize = 12.sp, color = SettingsSecondaryTextColor)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsPremiumToggleItem(title: String, subtitle: String, iconRes: Int, metrics: SettingsPremiumMetrics, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(metrics.itemHeight).clip(RoundedCornerShape(metrics.itemCornerRadius)).background(Color.White).border(1.dp, SettingsStrokeColor, RoundedCornerShape(metrics.itemCornerRadius)).clickable { onToggle(!checked) }.padding(horizontal = metrics.itemHorizontalPadding), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), null, modifier = Modifier.size(metrics.iconSize), colorFilter = ColorFilter.tint(SettingsAccentColor))
        Column(modifier = Modifier.weight(1f).padding(start = metrics.textStartPadding)) {
            Text(title, color = SettingsPrimaryTextColor, fontFamily = MontserratFontFamily, fontWeight = FontWeight.Bold, fontSize = metrics.titleFontSize)
            Text(subtitle, color = SettingsSecondaryTextColor, fontFamily = MontserratFontFamily, fontSize = metrics.subtitleFontSize, maxLines = 2)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

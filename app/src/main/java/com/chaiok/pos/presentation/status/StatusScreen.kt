package com.chaiok.pos.presentation.status

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val StatusBackgroundColor = Color.White
private val StatusPrimaryTextColor = Color(0xFF1B2128)
private val StatusSecondaryTextColor = Color(0xFF69707A)
private val StatusCardColor = Color(0xFFF7F8FA)
private val StatusFieldColor = Color(0xFFF7F8FA)
private val StatusAccentColor = Color(0xFF087BE8)
private val StatusSuccessColor = Color(0xFF14B8A6)

private val predefinedStatuses = listOf(
    "На смене",
    "Перерыв",
    "Занят",
    "Готов принимать гостей"
)

private data class StatusLayoutMetrics(
    val isSquareCompact: Boolean,

    val contentHorizontalPadding: Dp,
    val contentTopPadding: Dp,
    val contentBottomPadding: Dp,
    val contentSpacing: Dp,

    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val subtitleFontSize: TextUnit,
    val subtitleLineHeight: TextUnit,
    val subtitleMaxLines: Int,

    val fieldCornerRadius: Dp,
    val fieldShadowElevation: Dp,
    val fieldLabelFontSize: TextUnit,
    val fieldTextFontSize: TextUnit,

    val quickTitleFontSize: TextUnit,
    val quickTitleLineHeight: TextUnit,

    val optionsSpacing: Dp,
    val optionHeight: Dp,
    val optionCornerRadius: Dp,
    val optionSelectedShadowElevation: Dp,
    val optionDefaultShadowElevation: Dp,
    val optionHorizontalPadding: Dp,
    val optionIconBoxSize: Dp,
    val optionIconBoxCornerRadius: Dp,
    val optionIconSize: Dp,
    val optionTextStartPadding: Dp,
    val optionTitleFontSize: TextUnit,
    val optionTitleLineHeight: TextUnit,

    val saveButtonHeight: Dp,
    val saveButtonCornerRadius: Dp,
    val saveButtonEnabledShadowElevation: Dp,
    val saveButtonDisabledShadowElevation: Dp,
    val saveButtonFontSize: TextUnit,
    val saveButtonLineHeight: TextUnit,

    val successFontSize: TextUnit,
    val successLineHeight: TextUnit
)

@Composable
private fun statusLayoutMetrics(): StatusLayoutMetrics {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp

    return if (isSquareCompact) {
        StatusLayoutMetrics(
            isSquareCompact = true,

            contentHorizontalPadding = 14.dp,
            contentTopPadding = 10.dp,
            contentBottomPadding = 12.dp,
            contentSpacing = 8.dp,

            titleFontSize = 18.sp,
            titleLineHeight = 22.sp,
            subtitleFontSize = 12.sp,
            subtitleLineHeight = 16.sp,
            subtitleMaxLines = 2,

            fieldCornerRadius = 18.dp,
            fieldShadowElevation = 3.dp,
            fieldLabelFontSize = 11.sp,
            fieldTextFontSize = 13.sp,

            quickTitleFontSize = 15.sp,
            quickTitleLineHeight = 18.sp,

            optionsSpacing = 6.dp,
            optionHeight = 46.dp,
            optionCornerRadius = 18.dp,
            optionSelectedShadowElevation = 4.dp,
            optionDefaultShadowElevation = 3.dp,
            optionHorizontalPadding = 12.dp,
            optionIconBoxSize = 32.dp,
            optionIconBoxCornerRadius = 12.dp,
            optionIconSize = 18.dp,
            optionTextStartPadding = 10.dp,
            optionTitleFontSize = 13.sp,
            optionTitleLineHeight = 16.sp,

            saveButtonHeight = 44.dp,
            saveButtonCornerRadius = 18.dp,
            saveButtonEnabledShadowElevation = 5.dp,
            saveButtonDisabledShadowElevation = 2.dp,
            saveButtonFontSize = 14.sp,
            saveButtonLineHeight = 17.sp,

            successFontSize = 12.sp,
            successLineHeight = 15.sp
        )
    } else {
        StatusLayoutMetrics(
            isSquareCompact = false,

            contentHorizontalPadding = 24.dp,
            contentTopPadding = 28.dp,
            contentBottomPadding = 24.dp,
            contentSpacing = 14.dp,

            titleFontSize = 24.sp,
            titleLineHeight = 28.sp,
            subtitleFontSize = 14.sp,
            subtitleLineHeight = 19.sp,
            subtitleMaxLines = Int.MAX_VALUE,

            fieldCornerRadius = 24.dp,
            fieldShadowElevation = 5.dp,
            fieldLabelFontSize = 13.sp,
            fieldTextFontSize = 15.sp,

            quickTitleFontSize = 18.sp,
            quickTitleLineHeight = 22.sp,

            optionsSpacing = 10.dp,
            optionHeight = 72.dp,
            optionCornerRadius = 24.dp,
            optionSelectedShadowElevation = 8.dp,
            optionDefaultShadowElevation = 5.dp,
            optionHorizontalPadding = 16.dp,
            optionIconBoxSize = 42.dp,
            optionIconBoxCornerRadius = 16.dp,
            optionIconSize = 22.dp,
            optionTextStartPadding = 14.dp,
            optionTitleFontSize = 15.sp,
            optionTitleLineHeight = 19.sp,

            saveButtonHeight = 58.dp,
            saveButtonCornerRadius = 24.dp,
            saveButtonEnabledShadowElevation = 8.dp,
            saveButtonDisabledShadowElevation = 2.dp,
            saveButtonFontSize = 16.sp,
            saveButtonLineHeight = 20.sp,

            successFontSize = 14.sp,
            successLineHeight = 18.sp
        )
    }
}

@Composable
fun StatusScreen(
    state: StatusUiState,
    onBack: () -> Unit,
    onStatusChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    val metrics = statusLayoutMetrics()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StatusBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TiplyBackTopAppBar(
                title = "Статус",
                onBack = onBack
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
                    ),
                verticalArrangement = Arrangement.spacedBy(metrics.contentSpacing)
            ) {
                Text(
                    text = "Выбор статуса",
                    color = StatusPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.titleFontSize,
                    lineHeight = metrics.titleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Укажите статус, который будет виден гостям на главном экране.",
                    color = StatusSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = metrics.subtitleFontSize,
                    lineHeight = metrics.subtitleLineHeight,
                    maxLines = metrics.subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )

                StatusTextField(
                    value = state.selectedStatus,
                    metrics = metrics,
                    onValueChange = onStatusChanged
                )

                Text(
                    text = "Быстрый выбор",
                    color = StatusPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.quickTitleFontSize,
                    lineHeight = metrics.quickTitleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(metrics.optionsSpacing)
                ) {
                    predefinedStatuses.forEach { status ->
                        StatusOptionItem(
                            title = status,
                            selected = state.selectedStatus == status,
                            metrics = metrics,
                            onClick = { onStatusChanged(status) }
                        )
                    }
                }

                SaveStatusButton(
                    enabled = state.selectedStatus.isNotBlank(),
                    metrics = metrics,
                    onClick = onSave
                )

                state.successMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = StatusSuccessColor,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = metrics.successFontSize,
                        lineHeight = metrics.successLineHeight,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTextField(
    value: String,
    metrics: StatusLayoutMetrics,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.fieldShadowElevation,
                shape = RoundedCornerShape(metrics.fieldCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(metrics.fieldCornerRadius),
        singleLine = true,
        label = {
            Text(
                text = "Новый статус",
                fontFamily = MontserratFontFamily,
                fontSize = metrics.fieldLabelFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = StatusPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.fieldTextFontSize
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = StatusPrimaryTextColor,
            unfocusedTextColor = StatusPrimaryTextColor,
            focusedContainerColor = StatusFieldColor,
            unfocusedContainerColor = StatusFieldColor,
            cursorColor = StatusAccentColor,
            focusedBorderColor = StatusAccentColor,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = StatusAccentColor,
            unfocusedLabelColor = StatusSecondaryTextColor
        )
    )
}

@Composable
private fun StatusOptionItem(
    title: String,
    selected: Boolean,
    metrics: StatusLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.optionHeight)
            .shadow(
                elevation = if (selected) {
                    metrics.optionSelectedShadowElevation
                } else {
                    metrics.optionDefaultShadowElevation
                },
                shape = RoundedCornerShape(metrics.optionCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (selected) 0.10f else 0.07f),
                spotColor = Color.Black.copy(alpha = if (selected) 0.16f else 0.11f)
            )
            .clip(RoundedCornerShape(metrics.optionCornerRadius))
            .background(if (selected) Color(0xFFEAF7FF) else StatusCardColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = metrics.optionHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(metrics.optionIconBoxSize)
                .clip(RoundedCornerShape(metrics.optionIconBoxCornerRadius))
                .background(
                    if (selected) StatusAccentColor.copy(alpha = 0.14f)
                    else Color(0xFFF0F2F4)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Image(
                    painter = painterResource(id = R.drawable.ic_status_check),
                    contentDescription = "Выбрано",
                    modifier = Modifier.size(metrics.optionIconSize),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(StatusAccentColor)
                )
            }
        }

        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = metrics.optionTextStartPadding),
            color = StatusPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = metrics.optionTitleFontSize,
            lineHeight = metrics.optionTitleLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SaveStatusButton(
    enabled: Boolean,
    metrics: StatusLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.saveButtonHeight)
            .shadow(
                elevation = if (enabled) {
                    metrics.saveButtonEnabledShadowElevation
                } else {
                    metrics.saveButtonDisabledShadowElevation
                },
                shape = RoundedCornerShape(metrics.saveButtonCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (enabled) 0.12f else 0.04f),
                spotColor = Color.Black.copy(alpha = if (enabled) 0.18f else 0.06f)
            )
            .clip(RoundedCornerShape(metrics.saveButtonCornerRadius))
            .background(
                brush = if (enabled) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF087BE8),
                            Color(0xFF14B8A6)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFE2E4E7),
                            Color(0xFFD8DBDF)
                        )
                    )
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Сохранить статус",
            color = if (enabled) Color.White else StatusSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.saveButtonFontSize,
            lineHeight = metrics.saveButtonLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
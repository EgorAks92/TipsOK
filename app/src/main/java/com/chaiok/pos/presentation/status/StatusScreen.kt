package com.chaiok.pos.presentation.status

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val StatusBackgroundColor = Color.White
private val StatusPrimaryTextColor = Color(0xFF1B2128)
private val StatusSecondaryTextColor = Color(0xFF69707A)
private val StatusCardColor = Color(0xFFF7F8FA)
private val StatusFieldColor = Color(0xFFF7F8FA)
private val StatusAccentColor = Color(0xFF087BE8)
private val StatusSuccessColor = Color(0xFF14B8A6)
private val StatusStrokeColor = Color(0xFFE2E7EF)

private val predefinedStatuses = listOf(
    "На смене",
    "Перерыв",
    "Занят",
    "Готов принимать гостей"
)

private data class StatusPremiumMetrics(
    val contentHorizontalPadding: Dp,
    val contentTopPadding: Dp,
    val contentBottomPadding: Dp,

    val bottomAreaHorizontalPadding: Dp,
    val bottomAreaBottomPadding: Dp,
    val bottomAreaTopPadding: Dp,

    val headerTitleFontSize: TextUnit,
    val headerTitleLineHeight: TextUnit,
    val headerSubtitleFontSize: TextUnit,
    val headerSubtitleLineHeight: TextUnit,

    val sectionSpacing: Dp,
    val gridSpacing: Dp,
    val gridItemHeight: Dp,

    val optionCornerRadius: Dp,
    val optionShadowElevation: Dp,
    val optionHorizontalPadding: Dp,
    val optionIconBoxSize: Dp,
    val optionIconBoxCornerRadius: Dp,
    val optionIconSize: Dp,
    val optionTitleFontSize: TextUnit,
    val optionTitleLineHeight: TextUnit,

    val fieldHeight: Dp,
    val fieldCornerRadius: Dp,
    val fieldShadowElevation: Dp,
    val fieldLabelFontSize: TextUnit,
    val fieldTextFontSize: TextUnit,

    val saveButtonHeight: Dp,
    val saveButtonCornerRadius: Dp,
    val saveButtonShadowElevation: Dp,
    val saveButtonFontSize: TextUnit,
    val saveButtonLineHeight: TextUnit,

    val successFontSize: TextUnit,
    val successLineHeight: TextUnit
)

private fun squarePremiumStatusMetrics(): StatusPremiumMetrics {
    return StatusPremiumMetrics(
        contentHorizontalPadding = 16.dp,
        contentTopPadding = 12.dp,
        contentBottomPadding = 8.dp,

        bottomAreaHorizontalPadding = 16.dp,
        bottomAreaBottomPadding = 10.dp,
        bottomAreaTopPadding = 8.dp,

        headerTitleFontSize = 20.sp,
        headerTitleLineHeight = 24.sp,
        headerSubtitleFontSize = 12.sp,
        headerSubtitleLineHeight = 16.sp,

        sectionSpacing = 12.dp,
        gridSpacing = 9.dp,
        gridItemHeight = 64.dp,

        optionCornerRadius = 22.dp,
        optionShadowElevation = 4.dp,
        optionHorizontalPadding = 10.dp,
        optionIconBoxSize = 30.dp,
        optionIconBoxCornerRadius = 12.dp,
        optionIconSize = 17.dp,
        optionTitleFontSize = 13.sp,
        optionTitleLineHeight = 16.sp,

        fieldHeight = 56.dp,
        fieldCornerRadius = 20.dp,
        fieldShadowElevation = 4.dp,
        fieldLabelFontSize = 11.sp,
        fieldTextFontSize = 14.sp,

        saveButtonHeight = 44.dp,
        saveButtonCornerRadius = 18.dp,
        saveButtonShadowElevation = 5.dp,
        saveButtonFontSize = 14.sp,
        saveButtonLineHeight = 17.sp,

        successFontSize = 12.sp,
        successLineHeight = 15.sp
    )
}

@Composable
fun StatusScreen(
    state: StatusUiState,
    onBack: () -> Unit,
    onStatusChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    when (rememberChaiOkDeviceClass()) {
        ChaiOkDeviceClass.SquareCompact -> {
            StatusSquarePremiumScreen(
                state = state,
                onBack = onBack,
                onStatusChanged = onStatusChanged,
                onSave = onSave
            )
        }

        ChaiOkDeviceClass.Regular -> {
            StatusRegularScreen(
                state = state,
                onBack = onBack,
                onStatusChanged = onStatusChanged,
                onSave = onSave
            )
        }
    }
}

@Composable
private fun StatusRegularScreen(
    state: StatusUiState,
    onBack: () -> Unit,
    onStatusChanged: (String) -> Unit,
    onSave: () -> Unit
) {
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

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Выбор статуса",
                    color = StatusPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 28.sp
                )

                Text(
                    text = "Укажите статус, который будет виден гостям на главном экране.",
                    color = StatusSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                StatusRegularTextField(
                    value = state.selectedStatus,
                    onValueChange = onStatusChanged
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Быстрый выбор",
                    color = StatusPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    predefinedStatuses.forEach { status ->
                        StatusRegularOptionItem(
                            title = status,
                            selected = state.selectedStatus == status,
                            onClick = { onStatusChanged(status) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SaveRegularStatusButton(
                    enabled = state.selectedStatus.isNotBlank(),
                    onClick = onSave
                )

                state.successMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = StatusSuccessColor,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusSquarePremiumScreen(
    state: StatusUiState,
    onBack: () -> Unit,
    onStatusChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    val metrics = squarePremiumStatusMetrics()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StatusBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TiplyBackTopAppBar(
                title = "Статус",
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
                Text(
                    text = "Статус официанта",
                    color = StatusPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.headerTitleFontSize,
                    lineHeight = metrics.headerTitleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Быстро обновите статус для гостей",
                    color = StatusSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = metrics.headerSubtitleFontSize,
                    lineHeight = metrics.headerSubtitleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(metrics.sectionSpacing))

                StatusPremiumGrid(
                    selectedStatus = state.selectedStatus,
                    metrics = metrics,
                    onStatusChanged = onStatusChanged
                )

                Spacer(modifier = Modifier.height(metrics.sectionSpacing))

                StatusPremiumTextField(
                    value = state.selectedStatus,
                    metrics = metrics,
                    onValueChange = onStatusChanged
                )

                state.successMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = metrics.bottomAreaHorizontalPadding)
                    .padding(
                        top = metrics.bottomAreaTopPadding,
                        bottom = metrics.bottomAreaBottomPadding
                    )
            ) {
                SavePremiumStatusButton(
                    enabled = state.selectedStatus.isNotBlank(),
                    metrics = metrics,
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
private fun StatusPremiumGrid(
    selectedStatus: String,
    metrics: StatusPremiumMetrics,
    onStatusChanged: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(metrics.gridSpacing)
    ) {
        predefinedStatuses.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.gridSpacing)
            ) {
                rowItems.forEach { status ->
                    StatusPremiumOptionCard(
                        title = status,
                        selected = selectedStatus == status,
                        metrics = metrics,
                        modifier = Modifier.weight(1f),
                        onClick = { onStatusChanged(status) }
                    )
                }

                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatusPremiumOptionCard(
    title: String,
    selected: Boolean,
    metrics: StatusPremiumMetrics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val borderColor = if (selected) {
        StatusAccentColor.copy(alpha = 0.38f)
    } else {
        StatusStrokeColor.copy(alpha = 0.86f)
    }

    val backgroundBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                Color(0xFFEAF7FF),
                Color(0xFFEAFBF6)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                Color.White
            )
        )
    }

    Column(
        modifier = modifier
            .height(metrics.gridItemHeight)
            .shadow(
                elevation = metrics.optionShadowElevation,
                shape = RoundedCornerShape(metrics.optionCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (selected) 0.065f else 0.045f),
                spotColor = Color.Black.copy(alpha = if (selected) 0.12f else 0.09f)
            )
            .clip(RoundedCornerShape(metrics.optionCornerRadius))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(metrics.optionCornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = metrics.optionHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(metrics.optionIconBoxSize)
                .clip(RoundedCornerShape(metrics.optionIconBoxCornerRadius))
                .background(
                    if (selected) {
                        StatusAccentColor.copy(alpha = 0.15f)
                    } else {
                        StatusFieldColor
                    }
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

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = title,
            color = StatusPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = metrics.optionTitleFontSize,
            lineHeight = metrics.optionTitleLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusRegularTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        label = {
            Text(
                text = "Новый статус",
                fontFamily = MontserratFontFamily,
                fontSize = 13.sp
            )
        },
        textStyle = TextStyle(
            color = StatusPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
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
private fun StatusPremiumTextField(
    value: String,
    metrics: StatusPremiumMetrics,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.fieldHeight)
            .shadow(
                elevation = metrics.fieldShadowElevation,
                shape = RoundedCornerShape(metrics.fieldCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.045f),
                spotColor = Color.Black.copy(alpha = 0.09f)
            ),
        shape = RoundedCornerShape(metrics.fieldCornerRadius),
        singleLine = true,
        label = {
            Text(
                text = "Другой статус",
                fontFamily = MontserratFontFamily,
                fontSize = metrics.fieldLabelFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        textStyle = TextStyle(
            color = StatusPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.fieldTextFontSize
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = StatusPrimaryTextColor,
            unfocusedTextColor = StatusPrimaryTextColor,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = StatusAccentColor,
            focusedBorderColor = StatusAccentColor,
            unfocusedBorderColor = StatusStrokeColor.copy(alpha = 0.86f),
            focusedLabelColor = StatusAccentColor,
            unfocusedLabelColor = StatusSecondaryTextColor
        )
    )
}

@Composable
private fun StatusRegularOptionItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (selected) 8.dp else 5.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (selected) 0.10f else 0.07f),
                spotColor = Color.Black.copy(alpha = if (selected) 0.16f else 0.11f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) Color(0xFFEAF7FF) else StatusCardColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(16.dp))
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
                    modifier = Modifier.size(22.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(StatusAccentColor)
                )
            }
        }

        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            color = StatusPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SaveRegularStatusButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 2.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (enabled) 0.12f else 0.04f),
                spotColor = Color.Black.copy(alpha = if (enabled) 0.18f else 0.06f)
            )
            .clip(RoundedCornerShape(24.dp))
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
            fontSize = 16.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SavePremiumStatusButton(
    enabled: Boolean,
    metrics: StatusPremiumMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.saveButtonHeight)
            .shadow(
                elevation = if (enabled) metrics.saveButtonShadowElevation else 2.dp,
                shape = RoundedCornerShape(metrics.saveButtonCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (enabled) 0.08f else 0.035f),
                spotColor = Color.Black.copy(alpha = if (enabled) 0.14f else 0.055f)
            )
            .clip(RoundedCornerShape(metrics.saveButtonCornerRadius))
            .background(
                brush = if (enabled) {
                    Brush.linearGradient(
                        listOf(
                            StatusAccentColor,
                            StatusSuccessColor
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
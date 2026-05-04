package com.chaiok.pos.presentation.status

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
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

@Composable
fun StatusScreen(
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
            StatusTopAppBar(onBack = onBack)

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

                StatusTextField(
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
                        StatusOptionItem(
                            title = status,
                            selected = state.selectedStatus == status,
                            onClick = { onStatusChanged(status) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SaveStatusButton(
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
private fun StatusTopAppBar(
    onBack: () -> Unit
) {
    val barShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 46.dp,
        bottomEnd = 46.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 22.dp,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.20f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(barShape)
            .background(Color.White)
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 10.dp,
                bottom = 10.dp
            )
    ) {
        Text(
            text = "Статус",
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

        StatusTopIcon(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings_back),
                contentDescription = "Назад",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(StatusPrimaryTextColor)
            )
        }
    }
}

@Composable
private fun StatusTopIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun StatusTextField(
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
        textStyle = androidx.compose.ui.text.TextStyle(
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
private fun StatusOptionItem(
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
private fun SaveStatusButton(
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

package com.chaiok.pos.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

@Composable
fun TiplyNumericKeypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    isLoading: Boolean = false,
    touchSize: Dp = 86.dp,
    digitFontSize: TextUnit = 56.sp,
    rowSpacing: Dp = 0.dp,
    iconSize: Dp = 66.dp
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { label ->
                    TiplyKeypadDigit(
                        label = label,
                        onClick = { onDigit(label) },
                        touchSize = touchSize,
                        digitFontSize = digitFontSize
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TiplyKeypadIconButton(
                iconRes = R.drawable.ic_keypad_delete,
                contentDescription = "Удалить",
                onClick = onDelete,
                enabled = true,
                touchSize = touchSize,
                iconSize = iconSize
            )

            TiplyKeypadDigit(
                label = "0",
                onClick = { onDigit("0") },
                touchSize = touchSize,
                digitFontSize = digitFontSize
            )

            if (isLoading) {
                Box(modifier = Modifier.size(touchSize), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(0xFF1DE9E7),
                        strokeWidth = 2.6.dp,
                        modifier = Modifier.size(38.dp)
                    )
                }
            } else {
                TiplyKeypadIconButton(
                    iconRes = R.drawable.ic_keypad_confirm,
                    contentDescription = "Подтвердить",
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    touchSize = touchSize,
                    iconSize = iconSize
                )
            }
        }
    }
}

@Composable
private fun TiplyKeypadDigit(
    label: String,
    onClick: () -> Unit,
    touchSize: Dp,
    digitFontSize: TextUnit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(touchSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = digitFontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFontFamily,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = MontserratFontFamily,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.2f),
                    blurRadius = 6f
                )
            )
        )
    }
}

@Composable
private fun TiplyKeypadIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    touchSize: Dp,
    iconSize: Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(touchSize)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else 0.35f,
            modifier = Modifier.size(iconSize)
        )
    }
}

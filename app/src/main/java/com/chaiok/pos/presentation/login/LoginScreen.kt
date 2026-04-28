package com.chaiok.pos.presentation.login

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R

@Composable
fun LoginScreen(
    state: LoginUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onLogin: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val hasError = state.errorMessage != null
    val checkEnabled = state.pin.length == 4 && !state.isLoading

    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.triggerShake) {
        if (state.triggerShake > 0) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    -12f at 60
                    12f at 120
                    -10f at 180
                    10f at 240
                    0f at 350
                }
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(loginBackground(hasError))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Image(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Закрыть",
                    modifier = Modifier
                        .size(42.dp)
                        .clickable {
                            onClose?.invoke() ?: (context as? Activity)?.finish()
                        }
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tiply_logo),
                    contentDescription = "Tiply",
                    modifier = Modifier.fillMaxWidth(0.38f),
                    contentScale = ContentScale.FillWidth
                )

                Text(
                    text = "Введите PIN-код",
                    color = Color.White,
                    fontSize = 29.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                PinDots(
                    pinLength = state.pin.length,
                    isLoading = state.isLoading,
                    isError = hasError
                )

                state.errorMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFFFF8E95),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            LoginPinKeypad(
                isLoading = state.isLoading,
                checkEnabled = checkEnabled,
                onDigit = onDigit,
                onDelete = onDelete,
                onLogin = onLogin,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun loginBackground(hasError: Boolean): Brush {
    val bgTop = Color(0xFF141B23)
    val bgBottom = Color(0xFF111821)
    val glowCore = if (hasError) Color(0xA8FF3A43) else Color(0xAA1DE1DE)
    val glowMid = if (hasError) Color(0x75B31535) else Color(0x6D1A4FB0)

    return Brush.radialGradient(
        colors = listOf(glowCore, glowMid, bgBottom, bgTop),
        center = Offset(220f, 1100f),
        radius = 1450f
    )
}

@Composable
private fun PinDots(pinLength: Int, isLoading: Boolean, isError: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(4) { index ->
            val isFilled = index < pinLength
            val isActive = isError || isFilled
            val glowColor = if (isError) Color(0xFFFF4E58) else Color(0xFF22E3E5)

            val fillBrush = when {
                isError -> Brush.radialGradient(
                    colors = listOf(Color(0xFFFF8B90), Color(0xCCF14E57), Color(0x99B62B39))
                )

                isFilled -> Brush.radialGradient(
                    colors = listOf(Color(0xFF8CF8F5), Color(0xCC3ED6D6), Color(0xAA1EA5AF))
                )

                else -> Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.23f), Color(0x8066A3B2), Color(0x5A4B7385))
                )
            }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(60.dp)
                    .drawBehind {
                        val radius = size.minDimension / 2f
                        if (isActive) {
                            val glowBoost = if (isError || (pinLength == 4 || isLoading)) 1f else 0.65f
                            drawCircle(
                                color = glowColor.copy(alpha = 0.45f * glowBoost),
                                radius = radius * 1.52f
                            )
                            drawCircle(
                                color = glowColor.copy(alpha = 0.30f * glowBoost),
                                radius = radius * 1.26f
                            )
                            drawCircle(
                                color = glowColor.copy(alpha = 0.20f * glowBoost),
                                radius = radius * 1.08f
                            )
                        }

                        drawCircle(brush = fillBrush, radius = radius * 0.98f)

                        drawCircle(
                            color = Color.White.copy(alpha = if (isFilled || isError) 0.44f else 0.35f),
                            radius = radius * 0.98f,
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }
            )
        }
    }
}

@Composable
private fun LoginPinKeypad(
    isLoading: Boolean,
    checkEnabled: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { label ->
                    KeypadDigit(label = label) { onDigit(label) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeypadIconButton(
                iconRes = R.drawable.ic_keypad_delete,
                contentDescription = "Удалить",
                onClick = onDelete,
                enabled = true,
                size = 82.dp
            )

            KeypadDigit(label = "0") { onDigit("0") }

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF1DE9E7),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(42.dp)
                )
            } else {
                KeypadIconButton(
                    iconRes = R.drawable.ic_keypad_confirm,
                    contentDescription = "Подтвердить",
                    onClick = onLogin,
                    enabled = checkEnabled,
                    size = 82.dp
                )
            }
        }

        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun KeypadDigit(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 64.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.22f),
                blurRadius = 7f
            )
        ),
        modifier = Modifier
            .size(88.dp)
            .clickable(onClick = onClick)
            .padding(top = 6.dp)
    )
}

@Composable
private fun KeypadIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp
) {
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        alpha = if (enabled) 1f else 0.35f,
        modifier = Modifier
            .size(size)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp)
    )
}

package com.chaiok.pos.presentation.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    state: LoginUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onLogin: () -> Unit,
    onClose: (() -> Unit)? = null
) {
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

    val hasError = state.errorMessage != null

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(loginBackground(hasError))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        val checkEnabled = state.pin.length == 4 && !state.isLoading

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "×",
                    color = Color.White,
                    fontSize = 42.sp,
                    lineHeight = 42.sp,
                    modifier = Modifier
                        .clickable { onClose?.invoke() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LogoMark()
                Text(
                    text = "Введите PIN-код",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
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
                        color = Color(0xFFFF7D84),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
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
    val baseTop = Color(0xFF141B23)
    val baseBottom = Color(0xFF101722)
    val glowPrimary = if (hasError) Color(0x80FF3B3B) else Color(0x8022D7D6)
    val glowSecondary = if (hasError) Color(0x5CB11237) else Color(0x66306BFF)

    return Brush.radialGradient(
        colors = listOf(glowPrimary, glowSecondary, baseBottom, baseTop),
        center = androidx.compose.ui.geometry.Offset(220f, 1120f),
        radius = 1400f
    )
}

@Composable
private fun LogoMark() {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "●",
            color = Color(0xFF17D7E8),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 7.dp, end = 4.dp)
        )
        Text(
            text = "✓",
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 7.dp, end = 8.dp)
        )
        Text(
            text = "ЧайОК",
            color = Color.White,
            fontSize = 46.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PinDots(pinLength: Int, isLoading: Boolean, isError: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(4) { index ->
            val filled = index < pinLength
            val activeGlow = isError || (filled && (pinLength == 4 || isLoading))
            val dotColor = when {
                isError -> Color(0xFFFF6E74)
                filled -> Color(0xFF31D3D5)
                else -> Color(0x55739AAA)
            }
            val glowColor = if (isError) Color(0xFFFF404A) else Color(0xFF16E4E5)

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .drawBehind {
                        if (activeGlow) {
                            drawCircle(color = glowColor.copy(alpha = 0.42f), radius = size.minDimension * 0.80f)
                            drawCircle(color = glowColor.copy(alpha = 0.26f), radius = size.minDimension * 0.62f)
                        }
                        drawCircle(color = dotColor, radius = size.minDimension * 0.28f)
                        if (!filled && !isError) {
                            drawCircle(
                                color = Color(0x88C3E3EA),
                                radius = size.minDimension * 0.28f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                            )
                        }
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
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { label ->
                    KeypadNumber(label = label) { onDigit(label) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeypadAction(
                symbol = "⌫",
                tint = Color(0xFFFF6E79),
                glow = Color(0xFFFF4957),
                onClick = onDelete
            )

            KeypadNumber(label = "0") { onDigit("0") }

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF1DE9E7),
                    strokeWidth = 2.2.dp,
                    modifier = Modifier.size(38.dp)
                )
            } else {
                KeypadAction(
                    symbol = "✓",
                    tint = Color(0xFF1DE9E7),
                    glow = Color(0xFF13D6D5),
                    enabled = checkEnabled,
                    onClick = onLogin
                )
            }
        }

        Spacer(modifier = Modifier.width(1.dp))
    }
}

@Composable
private fun KeypadNumber(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 66.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(88.dp)
            .clickable(onClick = onClick)
            .padding(top = 6.dp),
        style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 6f))
    )
}

@Composable
private fun KeypadAction(
    symbol: String,
    tint: Color,
    glow: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = symbol,
        color = tint.copy(alpha = if (enabled) 1f else 0.35f),
        fontSize = 58.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        style = TextStyle(
            shadow = Shadow(
                color = glow.copy(alpha = if (enabled) 0.9f else 0.28f),
                blurRadius = if (enabled) 24f else 8f
            )
        ),
        modifier = Modifier
            .size(88.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(top = 8.dp)
    )
}

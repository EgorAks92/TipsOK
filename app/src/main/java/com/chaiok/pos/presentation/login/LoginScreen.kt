package com.chaiok.pos.presentation.login

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

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
            .drawBehind { drawLoginBackground(hasError = hasError) }
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val closeInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = { onClose?.invoke() ?: (context as? Activity)?.finish() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Закрыть",
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tiply_logo),
                    contentDescription = "Tiply",
                    modifier = Modifier.size(width = 124.dp, height = 58.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(52.dp))

                Text(
                    text = "Введите PIN-код",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MontserratFontFamily,
                        fontSize = 26.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(98.dp))

                PinDots(
                    pinLength = state.pin.length,
                    isLoading = state.isLoading,
                    isError = hasError
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            LoginPinKeypad(
                isLoading = state.isLoading,
                checkEnabled = checkEnabled,
                onDigit = onDigit,
                onDelete = onDelete,
                onLogin = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLoginBackground(hasError: Boolean) {
    val baseTop = Color(0xFF151B23)
    val baseBottom = Color(0xFF111821)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(baseTop, baseBottom)
        )
    )

    if (hasError) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x5EFF3A47),
                    Color(0x38CF2D45),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.55f, size.height * 0.62f),
                radius = size.maxDimension * 0.92f
            ),
            radius = size.maxDimension
        )
    } else {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x4A1DE7E2),
                    Color(0x2D1A8FAA),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.2f, size.height * 0.72f),
                radius = size.maxDimension * 0.95f
            ),
            radius = size.maxDimension
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x2D2A78B9),
                Color.Transparent
            ),
            center = Offset(size.width * 0.82f, size.height * 0.7f),
            radius = size.maxDimension * 0.78f
        ),
        radius = size.maxDimension
    )
}

@Composable
private fun PinDots(pinLength: Int, isLoading: Boolean, isError: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(23.dp)) {
        repeat(4) { index ->
            val isFilled = index < pinLength
            val active = isError || isFilled

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .drawBehind {
                        val coreRadius = 15.5.dp.toPx()
                        val glowRadius1 = 42.dp.toPx()
                        val glowRadius2 = 33.dp.toPx()
                        val glowRadius3 = 26.dp.toPx()

                        val glowColor = if (isError) Color(0xFFFF5E67) else Color(0xFF20E3DE)
                        if (active) {
                            val boost = if (isError || isLoading || pinLength == 4) 1f else 0.72f
                            drawCircle(color = glowColor.copy(alpha = 0.17f * boost), radius = glowRadius1)
                            drawCircle(color = glowColor.copy(alpha = 0.24f * boost), radius = glowRadius2)
                            drawCircle(color = glowColor.copy(alpha = 0.32f * boost), radius = glowRadius3)
                        }

                        val coreBrush = when {
                            isError -> Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF8C93),
                                    Color(0xFFE65E68),
                                    Color(0xFFB94453)
                                ),
                                center = Offset(size.width * 0.35f, size.height * 0.28f),
                                radius = coreRadius * 1.45f
                            )

                            isFilled -> Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF89F7F2),
                                    Color(0xFF49D4D3),
                                    Color(0xFF2F8FA6)
                                ),
                                center = Offset(size.width * 0.35f, size.height * 0.28f),
                                radius = coreRadius * 1.45f
                            )

                            else -> Brush.radialGradient(
                                colors = listOf(
                                    Color(0x667CC0CA),
                                    Color(0x55658A98),
                                    Color(0x4A4D6C7D)
                                ),
                                center = Offset(size.width * 0.35f, size.height * 0.28f),
                                radius = coreRadius * 1.55f
                            )
                        }

                        drawCircle(brush = coreBrush, radius = coreRadius)

                        drawCircle(
                            color = Color.White.copy(alpha = if (active) 0.3f else 0.26f),
                            radius = coreRadius,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                                center = Offset(size.width * 0.34f, size.height * 0.28f),
                                radius = coreRadius * 0.9f
                            ),
                            radius = coreRadius * 0.82f,
                            center = Offset(size.width * 0.4f, size.height * 0.36f)
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
                touchSize = 86.dp,
                iconSize = 66.dp
            )

            KeypadDigit(label = "0") { onDigit("0") }

            if (isLoading) {
                Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(0xFF1DE9E7),
                        strokeWidth = 2.6.dp,
                        modifier = Modifier.size(38.dp)
                    )
                }
            } else {
                KeypadIconButton(
                    iconRes = R.drawable.ic_keypad_confirm,
                    contentDescription = "Подтвердить",
                    onClick = onLogin,
                    enabled = checkEnabled,
                    touchSize = 86.dp,
                    iconSize = 66.dp
                )
            }
        }
    }
}

@Composable
private fun KeypadDigit(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(86.dp)
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
            fontSize = 56.sp,
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
private fun KeypadIconButton(
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

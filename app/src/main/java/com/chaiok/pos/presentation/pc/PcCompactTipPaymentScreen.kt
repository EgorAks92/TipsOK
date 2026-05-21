package com.chaiok.pos.presentation.pc

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlin.math.roundToInt

@Composable
fun PcCompactTipPaymentScreen(
    state: PcCompactTipPaymentUiState,
    onSelectTip: (Int) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    when (state.paymentStage) {
        CardPresentingStage.Approved -> PcCompactApprovedStateScreen(state.amountText)

        CardPresentingStage.Declined,
        CardPresentingStage.Error,
        CardPresentingStage.Cancelled -> PcCompactDeclinedStateScreen(
            amountText = state.amountText,
            errorMessage = state.errorMessage,
            onRetry = onRetry,
            onCancel = onCancel
        )

        CardPresentingStage.CardDetected,
        CardPresentingStage.Processing,
        CardPresentingStage.PinRequired,
        CardPresentingStage.Cancelling -> PcCompactProcessingStateScreen(state.amountText)

        else -> PcCompactTipSelectionStateScreen(
            state = state,
            onSelectTip = onSelectTip,
            onToggleServiceFee = onToggleServiceFee,
            onCancel = onCancel,
            onRetry = onRetry
        )
    }
}

@Composable
private fun PcCompactPaymentBackground(
    error: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = if (error) {
                        listOf(
                            Color(0xFF121923),
                            Color(0xFF2A1B26),
                            Color(0xFF161D27)
                        )
                    } else {
                        listOf(
                            Color(0xFF151D25),
                            Color(0xFF0E5C91),
                            Color(0xFF1B222A)
                        )
                    }
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (error) {
                            listOf(
                                Color(0xFFC8323A).copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFF126CA4).copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        },
                        center = Offset(110f, 420f),
                        radius = 520f
                    )
                )
        )

        content()
    }
}

@Composable
private fun PcCompactTipSelectionStateScreen(
    state: PcCompactTipPaymentUiState,
    onSelectTip: (Int) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    PcCompactPaymentBackground {
        PcCompactTopRings()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onCancel)
            )

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onCancel)
            )
        }

        Column(
            modifier = Modifier.padding(start = 32.dp, top = 136.dp)
        ) {
            Text(
                text = "к оплате",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MontserratFontFamily
            )

            Text(
                text = state.amountText,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MontserratFontFamily
            )

            if (state.isRestartingPayment) {
                Text(
                    text = "Обновляем сумму...",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontFamily = MontserratFontFamily
                )
            }
        }

        PcCompactDecorativeBankCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 78.dp)
                .offset(x = 26.dp)
        )

        Text(
            text = "чаевые",
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MontserratFontFamily,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 232.dp)
        )

        val tipsEnabled = state.canChangeTips && !state.isRestartingPayment

        LazyRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 260.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(state.availablePercents) { index, percent ->
                PcCompactTipPresetCard(
                    percentText = "${percent.roundToInt()}%",
                    amountText = formatRubles(state.billAmount * percent / 100.0),
                    selected = index == state.selectedPercentIndex,
                    enabled = tipsEnabled,
                    onClick = { onSelectTip(index) }
                )
            }
        }

        if (state.serviceFeePercent > 0.0) {
            PcCompactServiceFeeGlassRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                text = "Возмещение комиссии (${formatRubles(state.serviceFeeAmount)})",
                checked = state.isServiceFeeEnabled,
                enabled = tipsEnabled,
                onToggle = onToggleServiceFee
            )
        }

        if (state.errorMessage != null) {
            Text(
                text = "Повторить",
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clickable(onClick = onRetry)
            )
        }
    }
}

@Composable
private fun PcCompactProcessingStateScreen(
    amountText: String
) = PcCompactPaymentBackground {
    PcCompactCenteredAmountHeader(amountText)

    PcCompactProcessingSpinner(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 48.dp)
    )
}

@Composable
private fun PcCompactApprovedStateScreen(
    amountText: String
) = PcCompactPaymentBackground {
    PcCompactCenteredAmountHeader(amountText)

    PcCompactResultCheck(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 50.dp)
    )

    Text(
        text = "Одобрено",
        color = Color.White,
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = MontserratFontFamily,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 110.dp)
    )
}

@Composable
private fun PcCompactDeclinedStateScreen(
    amountText: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) = PcCompactPaymentBackground(error = true) {
    PcCompactCenteredAmountHeader(amountText)

    PcCompactResultCross(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 50.dp)
    )

    Text(
        text = "Отказано",
        color = Color.White,
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = MontserratFontFamily,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 110.dp)
    )

    if (!errorMessage.isNullOrBlank()) {
        Text(
            text = errorMessage,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 12.sp,
            fontFamily = MontserratFontFamily,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 78.dp
                )
        )
    }

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 34.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Повторить",
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.clickable(onClick = onRetry)
        )

        Text(
            text = "Отмена",
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.clickable(onClick = onCancel)
        )
    }
}

@Composable
private fun BoxScope.PcCompactCenteredAmountHeader(
    amountText: String
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 145.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "оплата",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 30.sp,
            fontFamily = MontserratFontFamily
        )

        Text(
            text = amountText,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MontserratFontFamily
        )
    }
}

@Composable
private fun PcCompactProcessingSpinner(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "spinner")

    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1300,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Canvas(
        modifier = modifier
            .size(120.dp)
            .graphicsLayer {
                rotationZ = rotation.value
            }
    ) {
        drawArc(
            color = Color(0x3320D6D2),
            startAngle = -90f,
            sweepAngle = 300f,
            useCenter = false,
            style = Stroke(
                width = 26.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        drawArc(
            color = Color(0xFF89E3EA).copy(alpha = 0.85f),
            startAngle = -90f,
            sweepAngle = 285f,
            useCenter = false,
            style = Stroke(
                width = 20.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun PcCompactResultCheck(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(140.dp)
    ) {
        drawLine(
            color = Color(0x4420D6D2),
            start = Offset(size.width * 0.22f, size.height * 0.54f),
            end = Offset(size.width * 0.44f, size.height * 0.74f),
            strokeWidth = 24.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0x4420D6D2),
            start = Offset(size.width * 0.44f, size.height * 0.74f),
            end = Offset(size.width * 0.8f, size.height * 0.3f),
            strokeWidth = 24.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF89E3EA),
            start = Offset(size.width * 0.22f, size.height * 0.54f),
            end = Offset(size.width * 0.44f, size.height * 0.74f),
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF89E3EA),
            start = Offset(size.width * 0.44f, size.height * 0.74f),
            end = Offset(size.width * 0.8f, size.height * 0.3f),
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PcCompactResultCross(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(140.dp)
    ) {
        drawLine(
            color = Color(0x44FF5454),
            start = Offset(size.width * 0.2f, size.height * 0.2f),
            end = Offset(size.width * 0.8f, size.height * 0.8f),
            strokeWidth = 24.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0x44FF5454),
            start = Offset(size.width * 0.8f, size.height * 0.2f),
            end = Offset(size.width * 0.2f, size.height * 0.8f),
            strokeWidth = 24.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFFFF9999),
            start = Offset(size.width * 0.2f, size.height * 0.2f),
            end = Offset(size.width * 0.8f, size.height * 0.8f),
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFFFF9999),
            start = Offset(size.width * 0.8f, size.height * 0.2f),
            end = Offset(size.width * 0.2f, size.height * 0.8f),
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PcCompactTopRings() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val c = Color(0xFF2D8FD4).copy(alpha = 0.18f)
        val center = Offset(size.width / 2f, -42.dp.toPx())

        drawCircle(
            color = c,
            radius = 58.dp.toPx(),
            center = center,
            style = Stroke(2.dp.toPx())
        )

        drawCircle(
            color = c,
            radius = 92.dp.toPx(),
            center = center,
            style = Stroke(2.dp.toPx())
        )

        drawCircle(
            color = c,
            radius = 132.dp.toPx(),
            center = center,
            style = Stroke(2.dp.toPx())
        )
    }
}

@Composable
private fun PcCompactDecorativeBankCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 180.dp, height = 110.dp)
            .graphicsLayer {
                rotationZ = 15f
            }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF747A80),
                        Color(0xFF4B5158)
                    )
                )
            )
            .alpha(0.85f)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            for (i in 1..4) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = (20 * i).dp.toPx(),
                    center = Offset(size.width * 0.18f, size.height * 0.45f),
                    style = Stroke(1.dp.toPx())
                )
            }
        }

        Text(
            text = "•••• 8724",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        )
    }
}

@Composable
private fun PcCompactServiceFeeGlassRow(
    modifier: Modifier,
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 14.sp,
            fontFamily = MontserratFontFamily
        )

        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (checked) {
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF20D6D2),
                                Color(0xFF126CA4)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.18f)
                            )
                        )
                    }
                )
                .clickable(enabled = enabled) {
                    onToggle(!checked)
                }
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .offset(x = if (checked) 24.dp else 0.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun PcCompactTipPresetCard(
    percentText: String,
    amountText: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    val backgroundBrush = if (selected) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF67E0DC),
                Color(0xFF20AFC2)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.11f)
            )
        )
    }

    Box(
        modifier = Modifier
            .size(
                width = 90.dp,
                height = if (selected) 140.dp else 124.dp
            )
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(
                elevation = if (selected) 10.dp else 3.dp,
                shape = shape
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (selected) 0.45f else 0.35f),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = percentText,
                color = Color.White,
                fontSize = if (selected) 24.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MontserratFontFamily
            )

            Text(
                text = amountText,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = if (selected) 16.sp else 15.sp,
                fontFamily = MontserratFontFamily
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
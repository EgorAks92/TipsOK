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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.cos
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.chaiok.pos.R
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.min
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay


private enum class PcCompactPaymentResultVisual {
    None,
    Approved,
    Declined
}

@Composable
fun PcCompactTipPaymentScreen(
    state: PcCompactTipPaymentUiState,
    onSelectTip: (Int) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val showDelayedProcessing = remember { mutableStateOf(false) }

    val processingRequested = !state.canChangeTips &&
        !state.isRestartingPayment &&
        (
            state.paymentStage == CardPresentingStage.CardDetected ||
                state.paymentStage == CardPresentingStage.Processing ||
                state.paymentStage == CardPresentingStage.PinRequired ||
                state.paymentStage == CardPresentingStage.Cancelling
            )

    val resultVisual = when {
        state.paymentStage == CardPresentingStage.Approved -> PcCompactPaymentResultVisual.Approved
        !state.canChangeTips &&
            !state.isRestartingPayment &&
            (
                state.paymentStage == CardPresentingStage.Declined ||
                    state.paymentStage == CardPresentingStage.Error ||
                    state.paymentStage == CardPresentingStage.Cancelled
                ) -> PcCompactPaymentResultVisual.Declined

        else -> PcCompactPaymentResultVisual.None
    }

    LaunchedEffect(processingRequested, resultVisual) {
        if (resultVisual != PcCompactPaymentResultVisual.None) {
            showDelayedProcessing.value = false
            return@LaunchedEffect
        }

        if (processingRequested) {
            delay(400)
            showDelayedProcessing.value = true
        } else {
            showDelayedProcessing.value = false
        }
    }

    val showTipSelection = state.canChangeTips || state.isRestartingPayment

    when {
        resultVisual != PcCompactPaymentResultVisual.None -> PcCompactPaymentStatusStateScreen(
            amountText = state.amountText,
            result = resultVisual,
            errorMessage = state.errorMessage,
            onRetry = onRetry,
            onCancel = onCancel
        )

        showTipSelection -> PcCompactTipSelectionStateScreen(
            state = state,
            onSelectTip = onSelectTip,
            onToggleServiceFee = onToggleServiceFee,
            onCancel = onCancel,
            onRetry = onRetry
        )

        showDelayedProcessing.value -> PcCompactPaymentStatusStateScreen(
            amountText = state.amountText,
            result = PcCompactPaymentResultVisual.None,
            errorMessage = state.errorMessage,
            onRetry = onRetry,
            onCancel = onCancel
        )

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clickable(onClick = onCancel)
            )
        }

        Column(
            modifier = Modifier.padding(start = 32.dp, top = 112.dp)
        ) {
            Text(
                text = "оплата",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MontserratFontFamily
            )

            PcCompactAnimatedAmountText(
                text = state.amountText,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MontserratFontFamily,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 228.dp)
        )

        val tipsClickable = state.canChangeTips && !state.isRestartingPayment
        val tipsVisuallyEnabled = true

        LazyRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 260.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = state.availablePercents,
                key = { _, percent -> percent.roundToInt() }
            ) { index, percent ->
                PcCompactTipPresetCard(
                    percentText = "${percent.roundToInt()}%",
                    amountText = formatRubles(state.billAmount * percent / 100.0),
                    selected = index == state.selectedPercentIndex,
                    enabled = tipsClickable,
                    visuallyEnabled = tipsVisuallyEnabled,
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
                enabled = tipsClickable,
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

        PcCompactAnimatedAmountText(
            text = amountText,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PcCompactPaymentStatusStateScreen(
    amountText: String,
    result: PcCompactPaymentResultVisual,
    errorMessage: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) = PcCompactPaymentBackground(error = result == PcCompactPaymentResultVisual.Declined) {
    PcCompactCenteredAmountHeader(amountText)

    PcCompactMorphingPaymentIndicator(
        result = result,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 50.dp)
    )

    when (result) {
        PcCompactPaymentResultVisual.Approved -> {
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

        PcCompactPaymentResultVisual.Declined -> {
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
                        .padding(start = 24.dp, end = 24.dp, bottom = 78.dp),
                    textAlign = TextAlign.Center
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

        PcCompactPaymentResultVisual.None -> Unit
    }
}

@Composable
private fun PcCompactMorphingPaymentIndicator(
    result: PcCompactPaymentResultVisual,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "morph_spinner")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morph_rot"
    )
    val morphProgress = remember { Animatable(0f) }

    LaunchedEffect(result) {
        if (result == PcCompactPaymentResultVisual.None) {
            morphProgress.snapTo(0f)
        } else {
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
            )
        }
    }

    val lineColor = if (result == PcCompactPaymentResultVisual.Declined) Color(0xFFFF9999) else Color(0xFF89E3EA)
    val glowColor = if (result == PcCompactPaymentResultVisual.Declined) Color(0xFFFF5454) else Color(0xFF20D6D2)

    Canvas(modifier = modifier.size(140.dp)) {
        val spinnerAlpha = 1f - morphProgress.value

        if (spinnerAlpha > 0f) {
            rotate(rotation.value) {
                drawArc(
                    color = glowColor.copy(alpha = 0.2f * spinnerAlpha),
                    startAngle = -90f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF89E3EA).copy(alpha = 0.85f * spinnerAlpha),
                    startAngle = -90f,
                    sweepAngle = 285f,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        when (result) {
            PcCompactPaymentResultVisual.Approved -> drawProgressiveCheck(morphProgress.value, lineColor, glowColor)
            PcCompactPaymentResultVisual.Declined -> drawProgressiveCross(morphProgress.value, lineColor, glowColor)
            PcCompactPaymentResultVisual.None -> Unit
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressiveCheck(
    progress: Float,
    lineColor: Color,
    glowColor: Color
) {
    val p1 = Offset(size.width * 0.22f, size.height * 0.54f)
    val p2 = Offset(size.width * 0.44f, size.height * 0.74f)
    val p3 = Offset(size.width * 0.8f, size.height * 0.3f)
    drawPartialLine(p1, p2, (progress * 2f).coerceIn(0f, 1f), glowColor.copy(alpha = 0.28f), 24.dp.toPx())
    drawPartialLine(p2, p3, ((progress - 0.5f) * 2f).coerceIn(0f, 1f), glowColor.copy(alpha = 0.28f), 24.dp.toPx())
    drawPartialLine(p1, p2, (progress * 2f).coerceIn(0f, 1f), lineColor, 18.dp.toPx())
    drawPartialLine(p2, p3, ((progress - 0.5f) * 2f).coerceIn(0f, 1f), lineColor, 18.dp.toPx())
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressiveCross(
    progress: Float,
    lineColor: Color,
    glowColor: Color
) {
    val p1 = Offset(size.width * 0.2f, size.height * 0.2f)
    val p2 = Offset(size.width * 0.8f, size.height * 0.8f)
    val p3 = Offset(size.width * 0.8f, size.height * 0.2f)
    val p4 = Offset(size.width * 0.2f, size.height * 0.8f)
    drawPartialLine(p1, p2, (progress * 2f).coerceIn(0f, 1f), glowColor.copy(alpha = 0.3f), 24.dp.toPx())
    drawPartialLine(p3, p4, ((progress - 0.5f) * 2f).coerceIn(0f, 1f), glowColor.copy(alpha = 0.3f), 24.dp.toPx())
    drawPartialLine(p1, p2, (progress * 2f).coerceIn(0f, 1f), lineColor, 18.dp.toPx())
    drawPartialLine(p3, p4, ((progress - 0.5f) * 2f).coerceIn(0f, 1f), lineColor, 18.dp.toPx())
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPartialLine(
    start: Offset,
    end: Offset,
    progress: Float,
    color: Color,
    strokeWidth: Float
) {
    if (progress <= 0f) return
    val p = progress.coerceIn(0f, 1f)
    val partialEnd = Offset(
        x = start.x + (end.x - start.x) * p,
        y = start.y + (end.y - start.y) * p
    )
    drawLine(color = color, start = start, end = partialEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

@Composable
private fun PcCompactTopRings() {
    val transition = rememberInfiniteTransition(label = "top_rings_premium")

    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "top_rings_phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val center = Offset(
            x = size.width / 2f,
            y = -44.dp.toPx()
        )

        val ringColor = Color(0xFF7DE8FF)
        val glowColor = Color(0xFF20D6D2)

        fun smoothWave(progress: Float): Float {
            val p = progress.coerceIn(0f, 1f)
            return 0.5f - 0.5f * cos((p * 2f * PI).toFloat())
        }

        fun ringProgress(start: Float, duration: Float): Float? {
            var raw = phase.value - start

            if (raw < 0f) {
                raw += 1f
            }

            return if (raw <= duration) {
                raw / duration
            } else {
                null
            }
        }

        val radiiDp = listOf(54f, 86f, 122f, 162f)
        val starts = listOf(0.00f, 0.17f, 0.34f, 0.51f)
        val maxAlphas = listOf(0.24f, 0.19f, 0.145f, 0.105f)
        val durations = listOf(0.34f, 0.36f, 0.38f, 0.40f)

        radiiDp.forEachIndexed { index, radiusDp ->
            val progress = ringProgress(
                start = starts[index],
                duration = durations[index]
            )

            if (progress != null) {
                val wave = smoothWave(progress)

                val radius = radiusDp.dp.toPx() + wave * 7.dp.toPx()
                val alpha = wave * maxAlphas[index]
                val glowAlpha = wave * maxAlphas[index] * 0.34f

                // Мягкое внешнее свечение кольца
                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Основная тонкая линия кольца
                drawCircle(
                    color = ringColor.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 1.6.dp.toPx() + wave * 0.55.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Внутренний дорогой highlight, почти незаметный
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.32f),
                    radius = radius - 1.5.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 0.7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Общее слабое свечение в центре, чтобы верх не выглядел пустым между волнами
        val ambient = 0.5f - 0.5f * cos((phase.value * 2f * PI).toFloat())

        drawCircle(
            color = glowColor.copy(alpha = 0.028f + ambient * 0.018f),
            radius = 118.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun PcCompactDecorativeBankCard(
    modifier: Modifier = Modifier
) {
    val enterProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        enterProgress.snapTo(0f)
        enterProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 850,
                delayMillis = 120,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
    }

    val progress = enterProgress.value
    val hidden = 1f - progress

    Image(
        painter = painterResource(id = R.drawable.pc_compact_bank_card),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(width = 190.dp, height = 122.dp)
            .offset(
                x = (22f * hidden).dp,
                y = (-6f * hidden).dp
            )
            .graphicsLayer {
                alpha = progress
                scaleX = 0.94f + 0.06f * progress
                scaleY = 0.94f + 0.06f * progress
                rotationZ = 18f - 3f * progress
            }
    )
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
private fun PcCompactAnimatedAmountText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.animateContentSize(
            animationSpec = tween(
                durationMillis = 260,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (
                            slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 260,
                                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                                )
                            ) { height -> height / 2 } + fadeIn(
                                animationSpec = tween(durationMillis = 180)
                            )
                            ).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(
                                    durationMillis = 220,
                                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                                )
                            ) { height -> -height / 2 } + fadeOut(
                                animationSpec = tween(durationMillis = 140)
                            )
                        ).using(
                            SizeTransform(clip = false)
                        )
                },
                label = "amount_char_$index",
                modifier = Modifier.clipToBounds()
            ) { animatedChar ->
                Text(
                    text = animatedChar.toString(),
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PcCompactTipPresetCard(
    percentText: String,
    amountText: String,
    selected: Boolean,
    enabled: Boolean,
    visuallyEnabled: Boolean = enabled,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val visualAlpha = if (visuallyEnabled) 1f else 0.5f

    val backgroundBrush = if (selected) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF67E0DC).copy(alpha = visualAlpha),
                Color(0xFF20AFC2).copy(alpha = visualAlpha)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f * visualAlpha),
                Color.White.copy(alpha = 0.11f * visualAlpha)
            )
        )
    }

    Box(
        modifier = Modifier
            .size(
                width = 90.dp,
                height = if (selected) 140.dp else 124.dp
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(
                    alpha = if (selected) 0.45f * visualAlpha else 0.35f * visualAlpha
                ),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = if (selected) 15.dp else 13.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                text = percentText,
                color = Color.White.copy(alpha = visualAlpha),
                fontSize = if (selected) 24.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MontserratFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = amountText,
                color = Color.White.copy(alpha = 0.92f * visualAlpha),
                fontSize = if (selected) 16.sp else 15.sp,
                fontFamily = MontserratFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(24.dp)
                ) {
                    val strokeWidth = 2.dp.toPx()
                    val radius = min(size.width, size.height) / 2f - strokeWidth / 2f

                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f * visualAlpha),
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = strokeWidth)
                    )
                }

                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = visualAlpha),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
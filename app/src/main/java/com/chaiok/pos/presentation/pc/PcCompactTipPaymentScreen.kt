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

@Composable
fun PcCompactTipPaymentScreen(
    state: PcCompactTipPaymentUiState,
    onSelectTip: (Int) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val showProcessingScreen = remember { mutableStateOf(false) }

    val isApproved = state.paymentStage == CardPresentingStage.Approved

    val isFailure = !state.canChangeTips &&
            !state.isRestartingPayment &&
            (
                    state.paymentStage == CardPresentingStage.Declined ||
                            state.paymentStage == CardPresentingStage.Error ||
                            state.paymentStage == CardPresentingStage.Cancelled
                    )

    val processingStageRequested = !state.canChangeTips &&
            !state.isRestartingPayment &&
            (
                    state.paymentStage == CardPresentingStage.CardDetected ||
                            state.paymentStage == CardPresentingStage.Processing ||
                            state.paymentStage == CardPresentingStage.PinRequired ||
                            state.paymentStage == CardPresentingStage.Cancelling
                    )

    LaunchedEffect(
        isApproved,
        isFailure,
        processingStageRequested
    ) {
        if (isApproved || isFailure) {
            showProcessingScreen.value = false
            return@LaunchedEffect
        }

        if (processingStageRequested) {
            // Защита от коротких внутренних SSP-состояний при смене чаевых.
            // Если это просто cancel/restart, экран выбора не успеет скрыться.
            delay(450)
            showProcessingScreen.value = true
        } else {
            showProcessingScreen.value = false
        }
    }

    when {
        isApproved -> PcCompactApprovedStateScreen(state.amountText)

        isFailure -> PcCompactDeclinedStateScreen(
            amountText = state.amountText,
            errorMessage = state.errorMessage,
            onRetry = onRetry,
            onCancel = onCancel
        )

        showProcessingScreen.value -> PcCompactProcessingStateScreen(state.amountText)

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
package com.chaiok.pos.presentation.pc

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaiok.pos.R
import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin

private enum class PcCompactPaymentResultVisual {
    None,
    Approved,
    Declined
}

@Composable
fun PcCompactTipPaymentScreen(
    state: PcCompactTipPaymentUiState,
    onSelectTip: (Int) -> Unit,
    onSelectNoTips: () -> Unit,
    onConfirmCustomTip: (Double) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val showStatusScreen = remember { mutableStateOf(false) }
    val visibleResultVisual = remember { mutableStateOf(PcCompactPaymentResultVisual.None) }
    val pendingResultVisual = remember { mutableStateOf<PcCompactPaymentResultVisual?>(null) }

    val processingRequested = !state.canChangeTips &&
            !state.isRestartingPayment &&
            (
                    state.paymentStage == CardPresentingStage.CardDetected ||
                            state.paymentStage == CardPresentingStage.Processing ||
                            state.paymentStage == CardPresentingStage.PinRequired ||
                            state.paymentStage == CardPresentingStage.Cancelling
                    )

    val realResultVisual = when {
        state.paymentStage == CardPresentingStage.Approved -> {
            PcCompactPaymentResultVisual.Approved
        }

        !state.canChangeTips &&
                !state.isRestartingPayment &&
                (
                        state.paymentStage == CardPresentingStage.Declined ||
                                state.paymentStage == CardPresentingStage.Error ||
                                state.paymentStage == CardPresentingStage.Cancelled
                        ) -> {
            PcCompactPaymentResultVisual.Declined
        }

        else -> PcCompactPaymentResultVisual.None
    }

    val showTipSelection = state.canChangeTips || state.isRestartingPayment

    LaunchedEffect(showTipSelection, processingRequested, realResultVisual) {
        if (showTipSelection && realResultVisual == PcCompactPaymentResultVisual.None) {
            showStatusScreen.value = false
            visibleResultVisual.value = PcCompactPaymentResultVisual.None
            pendingResultVisual.value = null
            return@LaunchedEffect
        }

        if (realResultVisual != PcCompactPaymentResultVisual.None) {
            if (!showStatusScreen.value) {
                showStatusScreen.value = true
                visibleResultVisual.value = PcCompactPaymentResultVisual.None
                pendingResultVisual.value = realResultVisual

                delay(420)

                if (pendingResultVisual.value == realResultVisual) {
                    visibleResultVisual.value = realResultVisual
                    pendingResultVisual.value = null
                }
            } else {
                visibleResultVisual.value = realResultVisual
                pendingResultVisual.value = null
            }

            return@LaunchedEffect
        }

        if (processingRequested) {
            delay(400)

            if (processingRequested && realResultVisual == PcCompactPaymentResultVisual.None) {
                showStatusScreen.value = true
                visibleResultVisual.value = PcCompactPaymentResultVisual.None
            }
        } else if (!showTipSelection) {
            showStatusScreen.value = false
            visibleResultVisual.value = PcCompactPaymentResultVisual.None
        }
    }

    when {
        showStatusScreen.value -> PcCompactPaymentStatusStateScreen(
            amountText = state.amountText,
            result = visibleResultVisual.value,
            errorMessage = state.errorMessage,
            onRetry = onRetry,
            onCancel = onCancel
        )

        showTipSelection -> PcCompactTipSelectionStateScreen(
            state = state,
            onSelectTip = onSelectTip,
            onSelectNoTips = onSelectNoTips,
            onConfirmCustomTip = onConfirmCustomTip,
            onToggleServiceFee = onToggleServiceFee,
            onCancel = onCancel,
            onRetry = onRetry
        )

        else -> PcCompactTipSelectionStateScreen(
            state = state,
            onSelectTip = onSelectTip,
            onSelectNoTips = onSelectNoTips,
            onConfirmCustomTip = onConfirmCustomTip,
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
    val backgroundEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    val bgStart by animateColorAsState(
        targetValue = if (error) Color(0xFF121923) else Color(0xFF151D25),
        animationSpec = tween(durationMillis = 520, easing = backgroundEasing),
        label = "pc_bg_start"
    )

    val bgMid by animateColorAsState(
        targetValue = if (error) Color(0xFF2A1B26) else Color(0xFF0E5C91),
        animationSpec = tween(durationMillis = 520, easing = backgroundEasing),
        label = "pc_bg_mid"
    )

    val bgEnd by animateColorAsState(
        targetValue = if (error) Color(0xFF161D27) else Color(0xFF1B222A),
        animationSpec = tween(durationMillis = 520, easing = backgroundEasing),
        label = "pc_bg_end"
    )

    val glowColor by animateColorAsState(
        targetValue = if (error) {
            Color(0xFFC8323A).copy(alpha = 0.55f)
        } else {
            Color(0xFF126CA4).copy(alpha = 0.55f)
        },
        animationSpec = tween(durationMillis = 520, easing = backgroundEasing),
        label = "pc_bg_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(bgStart, bgMid, bgEnd)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
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
    onSelectNoTips: () -> Unit,
    onConfirmCustomTip: (Double) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val showCustomTipDialog = remember { mutableStateOf(false) }

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

        val tipCards = buildList {
            add(PcCompactTipCardUiModel.CustomAmount)
            state.availablePercents.forEachIndexed { index, percent ->
                add(PcCompactTipCardUiModel.Percent(percent = percent, percentIndex = index))
            }
            add(PcCompactTipCardUiModel.NoTips)
        }

        LazyRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 260.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(items = tipCards, key = { _, card -> card.key }) { _, card ->
                when (card) {
                    PcCompactTipCardUiModel.CustomAmount -> PcCompactTipPresetCard(
                        percentText = "Своя",
                        amountText = if (state.isCustomTipSelected && state.customTipAmount != null) {
                            formatRubles(state.customTipAmount)
                        } else {
                            "сумма"
                        },
                        selected = state.isCustomTipSelected,
                        enabled = tipsClickable,
                        visuallyEnabled = tipsVisuallyEnabled,
                        onClick = { showCustomTipDialog.value = true }
                    )

                    is PcCompactTipCardUiModel.Percent -> PcCompactTipPresetCard(
                        percentText = "${card.percent.roundToInt()}%",
                        amountText = formatRubles(state.calculateTipByPercent(card.percent)),
                        selected = !state.isCustomTipSelected && !state.isNoTipsSelected && card.percentIndex == state.selectedPercentIndex,
                        enabled = tipsClickable,
                        visuallyEnabled = tipsVisuallyEnabled,
                        onClick = { onSelectTip(card.percentIndex) }
                    )

                    PcCompactTipCardUiModel.NoTips -> PcCompactTipPresetCard(
                        percentText = "0%",
                        amountText = "без чаевых",
                        amountFontSize = 13.sp,
                        selected = state.isNoTipsSelected,
                        enabled = tipsClickable,
                        visuallyEnabled = tipsVisuallyEnabled,
                        onClick = onSelectNoTips
                    )
                }
            }
        }
        if (showCustomTipDialog.value) {
            PcCompactCustomTipDialog(
                initialValue = customTipInputValue(state.customTipAmount),
                onDismiss = { showCustomTipDialog.value = false },
                onConfirm = { amount ->
                    showCustomTipDialog.value = false
                    onConfirmCustomTip(amount)
                }
            )
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

private sealed class PcCompactTipCardUiModel {
    object CustomAmount : PcCompactTipCardUiModel()
    data class Percent(val percent: Double, val percentIndex: Int) : PcCompactTipCardUiModel()
    object NoTips : PcCompactTipCardUiModel()

    val key: String
        get() = when (this) {
            CustomAmount -> "custom"
            is Percent -> "percent_$percentIndex"
            NoTips -> "no_tips"
        }
}

@Composable
private fun PcCompactCustomTipDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val value = remember(initialValue) { mutableStateOf(initialValue) }
    val normalized = value.value.filter(Char::isDigit).trimStart('0')
    val rubles = normalized.toIntOrNull() ?: 0
    val amount = rubles.toDouble()
    val confirmEnabled = rubles > 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.12f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF182B3D), Color(0xFF0F1D2A))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Своя сумма",
                    color = Color.White,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = formatRubles(amount),
                    color = Color(0xFF20D6D2),
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp)
                )
                TiplyNumericKeypad(
                    digitColor = Color.White,
                    touchSize = 44.dp,
                    digitFontSize = 20.sp,
                    iconSize = 22.dp,
                    onDigit = { digit ->
                        if (value.value.length < 6) {
                            val next = (value.value + digit).filter(Char::isDigit)
                            val nextRubles = next.trimStart('0').toIntOrNull() ?: 0
                            if (nextRubles <= CUSTOM_TIP_MAX_RUBLES) {
                                value.value = next
                            }
                        }
                    },
                    onDelete = { value.value = value.value.dropLast(1) },
                    onConfirm = { if (confirmEnabled) onConfirm(amount) },
                    confirmEnabled = confirmEnabled,
                    isLoading = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PcCompactDialogAction(
                        title = "Отмена",
                        primary = false,
                        enabled = true,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PcCompactDialogAction(
                        title = "Готово",
                        primary = true,
                        enabled = confirmEnabled,
                        onClick = { onConfirm(amount) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PcCompactDialogAction(
    title: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (primary) Color(0xFF1CC7BE) else Color.White.copy(alpha = 0.08f)
    val textColor = if (primary) Color.White else Color.White.copy(alpha = 0.95f)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) background else background.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = if (primary) Color(0xFF20D6D2) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFontFamily
        )
    }
}

private const val CUSTOM_TIP_MAX_RUBLES = 100_000

private fun customTipInputValue(amount: Double?): String {
    val rubles = amount?.toInt() ?: return ""
    return if (rubles > 0) rubles.toString() else ""
}

@Composable
private fun BoxScope.PcCompactCenteredAmountHeader(
    amountText: String
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "оплата",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 16.sp,
            fontFamily = MontserratFontFamily
        )

        PcCompactAnimatedAmountText(
            text = amountText,
            color = Color.White,
            fontSize = 40.sp,
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
        modifier = Modifier.align(Alignment.Center)
    )

    AnimatedVisibility(
        visible = result != PcCompactPaymentResultVisual.None,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 96.dp),
        enter = fadeIn(animationSpec = tween(280)) + slideInVertically(
            initialOffsetY = { it / 10 },
            animationSpec = tween(280)
        ),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        when (result) {
            PcCompactPaymentResultVisual.Approved -> {
                Text(
                    text = "Одобрено",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            PcCompactPaymentResultVisual.Declined -> {
                Text(
                    text = "Отказано",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            PcCompactPaymentResultVisual.None -> Unit
        }
    }

    AnimatedVisibility(
        visible = result == PcCompactPaymentResultVisual.Declined,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 34.dp),
        enter = fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = 170)) +
                slideInVertically(
                    initialOffsetY = { it / 8 },
                    animationSpec = tween(durationMillis = 280, delayMillis = 170)
                ),
        exit = fadeOut(animationSpec = tween(120))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = "Повторить",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontFamily = MontserratFontFamily,
                    modifier = Modifier.clickable(onClick = onRetry)
                )

                Text(
                    text = "Отмена",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontFamily = MontserratFontFamily,
                    modifier = Modifier.clickable(onClick = onCancel)
                )
            }
        }
    }
}

@Composable
private fun PcCompactMorphingPaymentIndicator(
    result: PcCompactPaymentResultVisual,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "neon_payment_indicator")

    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1450,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon_payment_indicator_rotation"
    )

    val morphProgress = remember { Animatable(0f) }
    val easing = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }

    LaunchedEffect(result) {
        if (result == PcCompactPaymentResultVisual.None) {
            morphProgress.snapTo(0f)
        } else {
            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 780,
                    easing = easing
                )
            )
        }
    }

    Canvas(
        modifier = modifier.size(144.dp)
    ) {
        val eased = easing.transform(morphProgress.value)

        val spinnerAlpha = (1f - eased).coerceIn(0f, 1f)
        val spinnerSweep = 292f - 230f * eased
        val spinnerRotation = rotation.value * (1f - eased * 0.28f)

        val resultProgress = ((eased - 0.14f) / 0.86f).coerceIn(0f, 1f)
        val settle = 1f + (1f - resultProgress) * 0.035f

        when (result) {
            PcCompactPaymentResultVisual.None -> {
                drawNeonSpinner(
                    rotation = rotation.value,
                    sweep = 292f,
                    alpha = 1f
                )
            }

            PcCompactPaymentResultVisual.Approved -> {
                if (spinnerAlpha > 0.001f) {
                    drawNeonSpinner(
                        rotation = spinnerRotation,
                        sweep = spinnerSweep,
                        alpha = spinnerAlpha
                    )
                }

                if (resultProgress > 0.001f) {
                    drawNeonCheck(
                        progress = resultProgress,
                        settle = settle
                    )
                }
            }

            PcCompactPaymentResultVisual.Declined -> {
                if (spinnerAlpha > 0.001f) {
                    drawNeonSpinner(
                        rotation = spinnerRotation,
                        sweep = spinnerSweep,
                        alpha = spinnerAlpha
                    )
                }

                if (resultProgress > 0.001f) {
                    drawNeonCross(
                        progress = resultProgress,
                        settle = settle
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNeonSpinner(
    rotation: Float,
    sweep: Float,
    alpha: Float
) {
    val center = Offset(size.width / 2f, size.height / 2f)

    val inset = 27.dp.toPx()
    val arcTopLeft = Offset(inset, inset)
    val arcSize = Size(
        width = size.width - inset * 2f,
        height = size.height - inset * 2f
    )

    // Очень мягкое внутреннее cyan-свечение, без грубого blob-пятна.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF19F1D4).copy(alpha = 0.16f * alpha),
                Color(0xFF0E8FD2).copy(alpha = 0.07f * alpha),
                Color.Transparent
            ),
            center = center,
            radius = size.minDimension * 0.42f
        ),
        radius = size.minDimension * 0.42f,
        center = center
    )

    rotate(
        degrees = rotation,
        pivot = center
    ) {
        drawNeonArc(
            topLeft = arcTopLeft,
            size = arcSize,
            startAngle = 34f,
            sweepAngle = sweep,
            alpha = alpha,
            glowColor = Color(0xFF16E8D3),
            edgeColor = Color(0xFF118BD7),
            coreColor = Color(0xFFCFFFF8)
        )
    }
}

private fun DrawScope.drawNeonCheck(
    progress: Float,
    settle: Float
) {
    val p = progress.coerceIn(0f, 1f)

    val a = Offset(size.width * 0.30f, size.height * 0.55f)
    val b = Offset(size.width * 0.43f, size.height * 0.67f)
    val c = Offset(size.width * 0.72f, size.height * 0.38f)

    val first = neonIntervalProgress(p, 0.00f, 0.42f)
    val second = neonIntervalProgress(p, 0.24f, 1.00f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1EF3D7).copy(alpha = 0.12f * p),
                Color(0xFF0E8FD2).copy(alpha = 0.04f * p),
                Color.Transparent
            ),
            center = Offset(size.width * 0.52f, size.height * 0.52f),
            radius = size.minDimension * 0.34f
        ),
        radius = size.minDimension * 0.34f,
        center = Offset(size.width * 0.52f, size.height * 0.52f)
    )

    // Пока галочка рисуется — оставляем по сегментам.
    drawNeonLine(
        start = a,
        end = b,
        progress = first,
        alpha = p,
        settle = settle,
        glowColor = Color(0xFF19F1D4),
        edgeColor = Color(0xFF118BD7),
        coreColor = Color(0xFFCFFFF8)
    )

    drawNeonLine(
        start = b,
        end = c,
        progress = second,
        alpha = p,
        settle = settle,
        glowColor = Color(0xFF19F1D4),
        edgeColor = Color(0xFF118BD7),
        coreColor = Color(0xFFCFFFF8)
    )

    // В конце поверх дорисовываем цельную галочку одним path,
    // чтобы исчезал видимый стык между двумя линиями.
    val unifiedAlpha = ((p - 0.78f) / 0.22f).coerceIn(0f, 1f)

    if (unifiedAlpha > 0f) {
        val checkPath = Path().apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
        }

        // Дальнее свечение
        drawPath(
            path = checkPath,
            color = Color(0xFF118BD7).copy(alpha = 0.040f * unifiedAlpha),
            style = Stroke(
                width = 52.dp.toPx() * settle,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Glow
        drawPath(
            path = checkPath,
            color = Color(0xFF19F1D4).copy(alpha = 0.070f * unifiedAlpha),
            style = Stroke(
                width = 42.dp.toPx() * settle,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawPath(
            path = checkPath,
            color = Color(0xFF19F1D4).copy(alpha = 0.145f * unifiedAlpha),
            style = Stroke(
                width = 32.dp.toPx() * settle,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Неоновое тело
        drawPath(
            path = checkPath,
            color = Color(0xFF19F1D4).copy(alpha = 0.36f * unifiedAlpha),
            style = Stroke(
                width = 23.dp.toPx() * settle,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Светлое ядро
        drawPath(
            path = checkPath,
            color = Color(0xFFCFFFF8).copy(alpha = 0.94f * unifiedAlpha),
            style = Stroke(
                width = 12.dp.toPx() * settle,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Тонкий highlight
        drawPath(
            path = checkPath,
            color = Color.White.copy(alpha = 0.20f * unifiedAlpha),
            style = Stroke(
                width = 5.dp.toPx() * settle,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun DrawScope.drawNeonCross(
    progress: Float,
    settle: Float
) {
    val p = progress.coerceIn(0f, 1f)

    val a = Offset(size.width * 0.30f, size.height * 0.30f)
    val b = Offset(size.width * 0.70f, size.height * 0.70f)

    val c = Offset(size.width * 0.70f, size.height * 0.30f)
    val d = Offset(size.width * 0.30f, size.height * 0.70f)

    val first = neonIntervalProgress(p, 0.00f, 0.58f)
    val second = neonIntervalProgress(p, 0.24f, 1.00f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF2A2A).copy(alpha = 0.10f * p),
                Color.Transparent
            ),
            center = Offset(size.width * 0.50f, size.height * 0.50f),
            radius = size.minDimension * 0.34f
        ),
        radius = size.minDimension * 0.34f,
        center = Offset(size.width * 0.50f, size.height * 0.50f)
    )

    drawNeonLine(
        start = a,
        end = b,
        progress = first,
        alpha = p,
        settle = settle,
        glowColor = Color(0xFFFF3030),
        edgeColor = Color(0xFFFF1F1F),
        coreColor = Color(0xFFFFA0A0)
    )

    drawNeonLine(
        start = c,
        end = d,
        progress = second,
        alpha = p,
        settle = settle,
        glowColor = Color(0xFFFF3030),
        edgeColor = Color(0xFFFF1F1F),
        coreColor = Color(0xFFFFA0A0)
    )
}

private fun DrawScope.drawNeonArc(
    topLeft: Offset,
    size: Size,
    startAngle: Float,
    sweepAngle: Float,
    alpha: Float,
    glowColor: Color,
    edgeColor: Color,
    coreColor: Color
) {
    // Дальнее мягкое свечение.
    drawArc(
        color = edgeColor.copy(alpha = 0.045f * alpha),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = 44.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    drawArc(
        color = glowColor.copy(alpha = 0.075f * alpha),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = 36.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    drawArc(
        color = glowColor.copy(alpha = 0.16f * alpha),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = 28.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // Насыщенное тело.
    drawArc(
        color = glowColor.copy(alpha = 0.34f * alpha),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = 22.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // Светлое ядро.
    drawArc(
        color = coreColor.copy(alpha = 0.90f * alpha),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = 13.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}

private fun DrawScope.drawNeonLine(
    start: Offset,
    end: Offset,
    progress: Float,
    alpha: Float,
    settle: Float,
    glowColor: Color,
    edgeColor: Color,
    coreColor: Color
) {
    val p = progress.coerceIn(0f, 1f)

    if (p <= 0f) return

    val currentEnd = Offset(
        x = start.x + (end.x - start.x) * p,
        y = start.y + (end.y - start.y) * p
    )

    // Дальнее синее/красное рассеивание.
    drawLine(
        color = edgeColor.copy(alpha = 0.040f * alpha),
        start = start,
        end = currentEnd,
        strokeWidth = 52.dp.toPx() * settle,
        cap = StrokeCap.Round
    )

    drawLine(
        color = glowColor.copy(alpha = 0.070f * alpha),
        start = start,
        end = currentEnd,
        strokeWidth = 42.dp.toPx() * settle,
        cap = StrokeCap.Round
    )

    drawLine(
        color = glowColor.copy(alpha = 0.145f * alpha),
        start = start,
        end = currentEnd,
        strokeWidth = 32.dp.toPx() * settle,
        cap = StrokeCap.Round
    )

    // Насыщенная неоновая труба.
    drawLine(
        color = glowColor.copy(alpha = 0.36f * alpha),
        start = start,
        end = currentEnd,
        strokeWidth = 23.dp.toPx() * settle,
        cap = StrokeCap.Round
    )

    // Светлое внутреннее ядро.
    drawLine(
        color = coreColor.copy(alpha = 0.94f * alpha),
        start = start,
        end = currentEnd,
        strokeWidth = 12.dp.toPx() * settle,
        cap = StrokeCap.Round
    )

    // Тонкий почти белый highlight — он и даёт ощущение неона.
    drawLine(
        color = Color.White.copy(alpha = 0.20f * alpha),
        start = start,
        end = currentEnd,
        strokeWidth = 5.dp.toPx() * settle,
        cap = StrokeCap.Round
    )
}

private fun neonIntervalProgress(
    progress: Float,
    start: Float,
    end: Float
): Float {
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
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

                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawCircle(
                    color = ringColor.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 1.6.dp.toPx() + wave * 0.55.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

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

package com.chaiok.pos.presentation.pc

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.chaiok.pos.R
import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.domain.model.PcEcrOperationType
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.Transition

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
    if (!state.visualSettingsLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
        return
    }

    val theme = rememberPcCompactPaymentVisualTheme(state.designStyle)
    ExistingPcCompactTipPaymentScreenContent(state, theme, onSelectTip, onSelectNoTips, onConfirmCustomTip, onToggleServiceFee, onCancel, onRetry)
}

private enum class PcCompactPaymentResultVisual {
    None,
    Approved,
    Declined
}

private enum class PcCompactPaymentScreenPhase {
    TipSelection,
    Processing,
    Approved,
    Declined
}

private enum class PcCompactResultIndicatorStyle {
    NeonDark,
    CleanLight
}

private val PC_COMPACT_TIP_HEADER_START = 32.dp
private val PC_COMPACT_TIP_HEADER_TOP = 112.dp
private val PC_COMPACT_STATUS_HEADER_TOP = 64.dp
private val PC_COMPACT_TIP_CARD_MIN_WIDTH = 140.dp
private val PC_COMPACT_TIP_CARD_MAX_WIDTH = 190.dp
private val PC_COMPACT_TIP_CARD_HEIGHT = 90.dp
private val PC_COMPACT_TIP_CARD_HORIZONTAL_PADDING = 12.dp
private val PC_COMPACT_TIP_CARD_TEXT_SAFETY_PADDING = 8.dp

@Composable
private fun ExistingPcCompactTipPaymentScreenContent(
    state: PcCompactTipPaymentUiState,
    theme: PcCompactPaymentVisualTheme,
    onSelectTip: (Int) -> Unit,
    onSelectNoTips: () -> Unit,
    onConfirmCustomTip: (Double) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val isCancelPrevious = state.operationType == PcEcrOperationType.CANCEL_PREVIOUS
    val showStatusScreen = remember { mutableStateOf(false) }
    val visibleResultVisual = remember { mutableStateOf(PcCompactPaymentResultVisual.None) }
    val pendingResultVisual = remember { mutableStateOf<PcCompactPaymentResultVisual?>(null) }

    val processingRequested =
        !state.isRestartingPayment &&
                state.paymentStage in setOf(
            CardPresentingStage.CardDetected,
            CardPresentingStage.Processing,
            CardPresentingStage.PinRequired,
            CardPresentingStage.Cancelling
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

    val showCancelPreviousWaitingForCard =
        isCancelPrevious &&
            !state.isRestartingPayment &&
            state.paymentStage in setOf(
                CardPresentingStage.Idle,
                CardPresentingStage.Preparing,
                CardPresentingStage.WaitingForCard
            )

    val showTipSelection =
        state.canChangeTips ||
            state.isRestartingPayment ||
            showCancelPreviousWaitingForCard

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

        delay(400)

        showStatusScreen.value = true
        visibleResultVisual.value = PcCompactPaymentResultVisual.None
    }

    val targetPhase = when {
        visibleResultVisual.value == PcCompactPaymentResultVisual.Approved -> PcCompactPaymentScreenPhase.Approved
        visibleResultVisual.value == PcCompactPaymentResultVisual.Declined -> PcCompactPaymentScreenPhase.Declined
        showStatusScreen.value -> PcCompactPaymentScreenPhase.Processing
        showTipSelection -> PcCompactPaymentScreenPhase.TipSelection
        else -> PcCompactPaymentScreenPhase.TipSelection
    }

    val phaseTransition = updateTransition(
        targetState = targetPhase,
        label = "pc_compact_payment_phase"
    )

    PcCompactPaymentAnimatedRoot(
        state = state,
        theme = theme,
        transition = phaseTransition,
        onSelectTip = onSelectTip,
        onSelectNoTips = onSelectNoTips,
        onConfirmCustomTip = onConfirmCustomTip,
        onToggleServiceFee = onToggleServiceFee,
        onCancel = onCancel,
        onRetry = onRetry
    )
}

@Composable
private fun PcCompactPaymentAnimatedRoot(
    state: PcCompactTipPaymentUiState,
    theme: PcCompactPaymentVisualTheme,
    transition: Transition<PcCompactPaymentScreenPhase>,
    onSelectTip: (Int) -> Unit,
    onSelectNoTips: () -> Unit,
    onConfirmCustomTip: (Double) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val phase = transition.targetState
    PcCompactPaymentBackground(error = phase == PcCompactPaymentScreenPhase.Declined, theme = theme) {
        PcCompactTipSelectionLayer(
            state = state,
            theme = theme,
            transition = transition,
            onSelectTip = onSelectTip,
            onSelectNoTips = onSelectNoTips,
            onConfirmCustomTip = onConfirmCustomTip,
            onToggleServiceFee = onToggleServiceFee,
            onCancel = onCancel,
            onRetry = onRetry
        )
        PcCompactAnimatedStatusHeader(
            amountText = state.amountText,
            operationTitle = state.operationTitle,
            transition = transition,
            theme = theme
        )
        PcCompactPaymentStatusOverlay(
            transition = transition,
            theme = theme,
            operationType = state.operationType
        )
        PcCompactPersistentCancelOverlay(
            state = state,
            theme = theme,
            onCancel = onCancel
        )
    }
}

@Composable
private fun BoxScope.PcCompactPersistentCancelOverlay(
    state: PcCompactTipPaymentUiState,
    theme: PcCompactPaymentVisualTheme,
    onCancel: () -> Unit
) {
    val show = state.canCancel
    if (!show) return

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 8.dp)
            .size(56.dp)
            .zIndex(10f)
            .clickable {
                Log.i(
                    "PcCompactTipPayment",
                    "UI cancel icon clicked operationType=${state.operationType} stage=${state.paymentStage} canCancel=${state.canCancel}"
                )
                onCancel()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = theme.closeIconDrawable),
            contentDescription = "Cancel operation",
            tint = theme.closeIconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PcCompactPaymentBackground(
    error: Boolean = false,
    theme: PcCompactPaymentVisualTheme,
    content: @Composable BoxScope.() -> Unit
) {
    if (!theme.useAnimatedDefaultBackground) {
        val background = if (error) theme.errorBackgroundBrush ?: theme.backgroundBrush else theme.backgroundBrush
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(theme.glowColor.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(110f, 420f),
                            radius = 520f
                        )
                    )
            )
            content()
        }
        return
    }

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
        targetValue = if (error) theme.declinedColor.copy(alpha = 0.55f) else theme.glowColor.copy(alpha = 0.55f),
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
private fun BoxScope.PcCompactAnimatedStatusHeader(
    amountText: String,
    operationTitle: String,
    transition: Transition<PcCompactPaymentScreenPhase>,
    theme: PcCompactPaymentVisualTheme
) {
    val premiumEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val density = LocalDensity.current
    var headerSize by remember { mutableStateOf(IntSize.Zero) }
    var titleSize by remember { mutableStateOf(IntSize.Zero) }

    val phaseProgress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 520, easing = premiumEasing) },
        label = "pc_status_header_progress"
    ) { phase ->
        if (phase == PcCompactPaymentScreenPhase.TipSelection) 0f else 1f
    }

    val headerAlpha by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 260,
                delayMillis = 50,
                easing = premiumEasing
            )
        },
        label = "pc_status_header_alpha"
    ) { phase ->
        if (phase == PcCompactPaymentScreenPhase.TipSelection) 0f else 1f
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentWidthPx = with(density) { maxWidth.toPx() }
        val startXPx = with(density) { PC_COMPACT_TIP_HEADER_START.toPx() }
        val startYPx = with(density) { PC_COMPACT_TIP_HEADER_TOP.toPx() }
        val endYPx = with(density) { PC_COMPACT_STATUS_HEADER_TOP.toPx() }
        val centeredXPx = ((parentWidthPx - headerSize.width) / 2f).coerceAtLeast(0f)
        val animatedXPx = startXPx + (centeredXPx - startXPx) * phaseProgress
        val animatedYPx = startYPx + (endYPx - startYPx) * phaseProgress
        val titleCenteredXPx = ((headerSize.width - titleSize.width) / 2f).coerceAtLeast(0f)
        val animatedTitleXPx = titleCenteredXPx * phaseProgress

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .onGloballyPositioned { coordinates -> headerSize = coordinates.size }
                .graphicsLayer {
                    alpha = headerAlpha
                    translationX = animatedXPx
                    translationY = animatedYPx
                },
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = if (operationTitle == "сверка итогов") operationTitle else operationTitle.lowercase(),
                color = theme.secondaryTextColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MontserratFontFamily,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .onGloballyPositioned { coordinates -> titleSize = coordinates.size }
                    .graphicsLayer {
                        translationX = animatedTitleXPx
                    }
            )

            if (amountText.isNotBlank()) {
                PcCompactAnimatedAmountText(
                    text = amountText,
                    color = theme.primaryTextColor,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BoxScope.PcCompactTipSelectionLayer(
    state: PcCompactTipPaymentUiState,
    theme: PcCompactPaymentVisualTheme,
    transition: Transition<PcCompactPaymentScreenPhase>,
    onSelectTip: (Int) -> Unit,
    onSelectNoTips: () -> Unit,
    onConfirmCustomTip: (Double) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val isCancelPrevious = state.operationType == PcEcrOperationType.CANCEL_PREVIOUS
    val isReconciliation = state.operationType == PcEcrOperationType.RECONCILIATION
    val phase = transition.targetState
    val showCustomTipDialog = remember { mutableStateOf(false) }
    val premiumEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    LaunchedEffect(phase) {
        if (phase != PcCompactPaymentScreenPhase.TipSelection) {
            showCustomTipDialog.value = false
        }
    }
    LaunchedEffect(state.tipConfigLoaded, state.showCustomTipButton) {
        if (!state.tipConfigLoaded || !state.showCustomTipButton) {
            showCustomTipDialog.value = false
        }
    }

    val tipsContentAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 420, easing = premiumEasing) },
        label = "pc_tips_alpha"
    ) { targetPhase ->
        if (targetPhase == PcCompactPaymentScreenPhase.TipSelection) 1f else 0f
    }

    val tipsContentOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 420, easing = premiumEasing) },
        label = "pc_tips_offset_y"
    ) { targetPhase ->
        if (targetPhase == PcCompactPaymentScreenPhase.TipSelection) 0.dp else 20.dp
    }

    val tipsContentScale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 420, easing = premiumEasing) },
        label = "pc_tips_scale"
    ) { targetPhase ->
        if (targetPhase == PcCompactPaymentScreenPhase.TipSelection) 1f else 0.985f
    }

    val tipsInteractive = phase == PcCompactPaymentScreenPhase.TipSelection &&
            state.canChangeTips &&
            !state.isRestartingPayment

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = tipsContentAlpha
                translationY = tipsContentOffsetY.toPx()
                scaleX = tipsContentScale
                scaleY = tipsContentScale
            }
    ) {
        PcCompactTipSelectionWavesLayer(transition = transition, theme = theme)

        Column(
            modifier = Modifier.padding(start = 32.dp, top = 158.dp)
        ) {
            Text(
                text = if (state.operationType == PcEcrOperationType.RECONCILIATION) state.operationTitle else state.operationTitle.lowercase(),
                color = theme.secondaryTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MontserratFontFamily
            )

            if (state.amountText.isNotBlank()) {
                PcCompactAnimatedAmountText(
                    text = state.amountText,
                    color = theme.primaryTextColor,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        PcCompactDecorativeBankCard(
            theme = theme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 78.dp)
                .offset(x = 26.dp)
        )

        if (isCancelPrevious) {
            val title = when (state.paymentStage) {
                CardPresentingStage.Preparing -> "Подготовка отмены"
                CardPresentingStage.WaitingForCard -> "Предъявите карту"
                else -> "Предъявите карту"
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 300.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = theme.primaryTextColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "для отмены операции",
                    color = theme.secondaryTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        }

        val showServiceFeeRow = !isCancelPrevious && !isReconciliation && state.showServiceFeeToggle && state.serviceFeePercent > 0.0
        val tipsTitleTop = if (showServiceFeeRow) 214.dp else 262.dp
        val tipsRowTop = if (showServiceFeeRow) 244.dp else 302.dp

        if (!isCancelPrevious && !isReconciliation) Text(
            text = "чаевые",
            color = theme.secondaryTextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MontserratFontFamily,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = tipsTitleTop)
        )



        val tipsVisuallyEnabled = true

        val tipCards = buildList {
            if (isCancelPrevious || isReconciliation) return@buildList
            if (state.tipConfigLoaded && state.showCustomTipButton) {
                add(PcCompactTipCardUiModel.CustomAmount)
            }
            state.availablePercents.forEachIndexed { index, percent ->
                add(PcCompactTipCardUiModel.Percent(percent = percent, percentIndex = index))
            }
        }
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val cardPrimaryTexts = tipCards.map { it.resolvePrimaryText(state) }
        val resolvedTipCardWidth = remember(cardPrimaryTexts, density) {
            val textStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MontserratFontFamily
            )
            val maxTextWidthDp = cardPrimaryTexts
                .map { text ->
                    val singleLineText = text.replace('\n', ' ')
                    val measuredWidthPx = textMeasurer.measure(
                        text = singleLineText,
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false
                    ).size.width
                    with(density) { measuredWidthPx.toDp() }
                }
                .maxOrNull()
                ?: 0.dp

            val desiredWidth = maxTextWidthDp +
                    PC_COMPACT_TIP_CARD_HORIZONTAL_PADDING * 2 +
                    PC_COMPACT_TIP_CARD_TEXT_SAFETY_PADDING

            desiredWidth.coerceIn(PC_COMPACT_TIP_CARD_MIN_WIDTH, PC_COMPACT_TIP_CARD_MAX_WIDTH)
        }
        val tipCardKeysSignature = tipCards.joinToString("|") { it.key }

        val middleIndex = tipCards.size / 2
        val listState = rememberLazyListState()

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = tipsRowTop)
        ) {
            val carouselViewportWidth = maxWidth

            val viewportWidthPx = with(density) { carouselViewportWidth.toPx() }
            val cardWidthPx = with(density) { resolvedTipCardWidth.toPx() }
            val centerOffsetPx = -((viewportWidthPx - cardWidthPx) / 2f).roundToInt()

            LaunchedEffect(tipCardKeysSignature, centerOffsetPx, resolvedTipCardWidth) {
                if (tipCards.isNotEmpty()) {
                    listState.scrollToItem(
                        index = middleIndex,
                        scrollOffset = centerOffsetPx
                    )
                }
            }

            if (tipCards.isNotEmpty()) LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(
                    items = tipCards,
                    key = { _, card -> card.key }
                ) { _, card ->
                    val primaryText = card.resolvePrimaryText(state)

                    when (card) {
                        PcCompactTipCardUiModel.CustomAmount -> {
                            PcCompactTipPresetCard(
                                theme = theme,
                                percentText = primaryText,
                                amountText = null,
                                selected = state.isCustomTipSelected,
                                enabled = tipsInteractive,
                                visuallyEnabled = tipsVisuallyEnabled,
                                cardWidth = resolvedTipCardWidth,
                                onClick = {
                                    if (tipsInteractive && state.tipConfigLoaded && state.showCustomTipButton) {
                                        showCustomTipDialog.value = true
                                    }
                                }
                            )
                        }

                        is PcCompactTipCardUiModel.Percent -> {
                            PcCompactTipPresetCard(
                                theme = theme,
                                percentText = primaryText,
                                amountText = null,
                                selected = !state.isCustomTipSelected &&
                                        !state.isNoTipsSelected &&
                                        card.percentIndex == state.selectedPercentIndex,
                                enabled = tipsInteractive,
                                visuallyEnabled = tipsVisuallyEnabled,
                                cardWidth = resolvedTipCardWidth,
                                onClick = { onSelectTip(card.percentIndex) }
                            )
                        }
                    }
                }
            }
        }
        val noTipsButtonGap = if (showServiceFeeRow) 10.dp else 20.dp
        val noTipsButtonTop = tipsRowTop + PC_COMPACT_TIP_CARD_HEIGHT + noTipsButtonGap
        if (!isCancelPrevious && !isReconciliation) PcCompactNoTipsButton(
            theme = theme,
            selected = state.isNoTipsSelected,
            enabled = tipsInteractive,
            visuallyEnabled = tipsVisuallyEnabled,
            onClick = onSelectNoTips,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = noTipsButtonTop)
        )
        if (!isCancelPrevious && showServiceFeeRow) {
            PcCompactServiceFeeGlassRow(
                theme = theme,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                text = "Возмещение комиссии (${formatRubles(state.serviceFeeAmount)})",
                checked = state.isServiceFeeEnabled,
                enabled = tipsInteractive,
                onToggle = onToggleServiceFee
            )
        }

        if (state.errorMessage != null) {
            Text(
                text = "Повторить",
                color = theme.primaryTextColor.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clickable(enabled = tipsInteractive, onClick = onRetry)
            )
        }
    }

    if (
        !isCancelPrevious &&
        showCustomTipDialog.value &&
        phase == PcCompactPaymentScreenPhase.TipSelection &&
        state.tipConfigLoaded &&
        state.showCustomTipButton
    ) {
        PcCompactCustomTipDialog(
            initialValue = customTipInputValue(state.customTipAmount),
            onDismiss = { showCustomTipDialog.value = false },
            onConfirm = { amount ->
                showCustomTipDialog.value = false
                onConfirmCustomTip(amount)
            }
        )
    }
}

@Composable
private fun BoxScope.PcCompactTipSelectionWavesLayer(
    transition: Transition<PcCompactPaymentScreenPhase>,
    theme: PcCompactPaymentVisualTheme
) {
    val premiumEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val wavesAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 260, easing = premiumEasing) },
        label = "pc_waves_alpha"
    ) { phase ->
        if (phase == PcCompactPaymentScreenPhase.TipSelection) 1f else 0f
    }

    if (wavesAlpha > 0.001f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = wavesAlpha }
        ) {
            PcCompactTopRings(alpha = wavesAlpha, theme = theme)
        }
    }
}

private sealed class PcCompactTipCardUiModel {
    object CustomAmount : PcCompactTipCardUiModel()
    data class Percent(val percent: Double, val percentIndex: Int) : PcCompactTipCardUiModel()

    val key: String
        get() = when (this) {
            CustomAmount -> "custom"
            is Percent -> "percent_$percentIndex"
        }
}

private fun PcCompactTipCardUiModel.resolvePrimaryText(
    state: PcCompactTipPaymentUiState
): String = when (this) {
    PcCompactTipCardUiModel.CustomAmount -> {
        if (state.isCustomTipSelected && state.customTipAmount != null) {
            formatRubles(state.customTipAmount)
        } else {
            "Своя\nсумма"
        }
    }

    is PcCompactTipCardUiModel.Percent -> {
        formatRubles(state.calculateTipByPercent(percent))
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF182B3D), Color(0xFF0F1D2A))
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(28.dp)
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
private fun BoxScope.PcCompactPaymentStatusOverlay(
    transition: Transition<PcCompactPaymentScreenPhase>,
    theme: PcCompactPaymentVisualTheme,
    operationType: PcEcrOperationType
) {
    val premiumEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val overlayAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 470, easing = premiumEasing) },
        label = "pc_status_overlay_alpha"
    ) { phase ->
        if (phase == PcCompactPaymentScreenPhase.TipSelection) 0f else 1f
    }

    val overlayScale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 470, easing = premiumEasing) },
        label = "pc_status_overlay_scale"
    ) { phase ->
        if (phase == PcCompactPaymentScreenPhase.TipSelection) 0.92f else 1f
    }

    val overlayOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 470, easing = premiumEasing) },
        label = "pc_status_overlay_offset"
    ) { phase ->
        if (phase == PcCompactPaymentScreenPhase.TipSelection) 16.dp else 0.dp
    }

    val resultVisual = when (transition.targetState) {
        PcCompactPaymentScreenPhase.Processing,
        PcCompactPaymentScreenPhase.TipSelection -> PcCompactPaymentResultVisual.None
        PcCompactPaymentScreenPhase.Approved -> PcCompactPaymentResultVisual.Approved
        PcCompactPaymentScreenPhase.Declined -> PcCompactPaymentResultVisual.Declined
    }

    PcCompactMorphingPaymentIndicator(
        result = resultVisual,
        theme = theme,
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer {
                alpha = overlayAlpha
                scaleX = overlayScale
                scaleY = overlayScale
                translationY = overlayOffsetY.toPx()
            }
    )

    AnimatedVisibility(
        visible = resultVisual != PcCompactPaymentResultVisual.None,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 96.dp),
        enter = fadeIn(animationSpec = tween(280)) + slideInVertically(
            initialOffsetY = { it / 10 },
            animationSpec = tween(280)
        ),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        when (resultVisual) {
            PcCompactPaymentResultVisual.Approved -> {
                Text(
                    text = if (operationType == PcEcrOperationType.RECONCILIATION) "Успешно" else "Одобрено",
                    color = theme.primaryTextColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            PcCompactPaymentResultVisual.Declined -> {
                Text(
                    text = "Отказано",
                    color = theme.primaryTextColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            PcCompactPaymentResultVisual.None -> Unit
        }
    }
}

@Composable
private fun PcCompactMorphingPaymentIndicator(
    result: PcCompactPaymentResultVisual,
    theme: PcCompactPaymentVisualTheme,
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
                    durationMillis = 920,
                    easing = easing
                )
            )
        }
    }

    Canvas(
        modifier = modifier.size(144.dp)
    ) {
        val p = easing.transform(morphProgress.value)

        val spinnerPhase = (1f - (p / 0.42f)).coerceIn(0f, 1f)
        val bridgeIn = ((p - 0.18f) / 0.28f).coerceIn(0f, 1f)
        val bridgeOut = 1f - ((p - 0.52f) / 0.28f).coerceIn(0f, 1f)
        val bridgeProgress = bridgeIn
        val bridgeAlpha = bridgeIn * bridgeOut

        val spinnerAlpha = (1f - p * 1.35f).coerceIn(0f, 1f)
        val spinnerSweep = 292f - 250f * p
        val spinnerRotation = rotation.value * (1f - p * 0.42f)
        val spinnerScale = 1f - 0.045f * p

        val resultProgress = ((p - 0.28f) / 0.72f).coerceIn(0f, 1f)
        val settle = 1f + (1f - resultProgress) * 0.035f

        when (result) {
            PcCompactPaymentResultVisual.None -> {
                drawNeonSpinner(
                    rotation = rotation.value,
                    sweep = 292f,
                    alpha = 1f,
                    scale = 1f,
                    theme = theme
                )
            }

            PcCompactPaymentResultVisual.Approved -> {
                if (spinnerPhase > 0.001f && spinnerAlpha > 0.001f) {
                    drawNeonSpinner(
                        rotation = spinnerRotation,
                        sweep = spinnerSweep,
                        alpha = spinnerAlpha * (0.55f + 0.45f * spinnerPhase),
                        scale = spinnerScale,
                        theme = theme
                    )
                }

                if (bridgeAlpha > 0.001f) {
                    drawMorphBridgeStroke(
                        progress = bridgeProgress,
                        alpha = bridgeAlpha,
                        approved = true,
                        settle = settle,
                        theme = theme
                    )
                }

                if (resultProgress > 0.001f) {
                    drawNeonCheck(
                        progress = resultProgress,
                        settle = settle,
                        theme = theme
                    )
                }
            }

            PcCompactPaymentResultVisual.Declined -> {
                if (spinnerPhase > 0.001f && spinnerAlpha > 0.001f) {
                    drawNeonSpinner(
                        rotation = spinnerRotation,
                        sweep = spinnerSweep,
                        alpha = spinnerAlpha * (0.55f + 0.45f * spinnerPhase),
                        scale = spinnerScale,
                        theme = theme
                    )
                }

                if (bridgeAlpha > 0.001f) {
                    drawMorphBridgeStroke(
                        progress = bridgeProgress,
                        alpha = bridgeAlpha,
                        approved = false,
                        settle = settle,
                        theme = theme
                    )
                }

                if (resultProgress > 0.001f) {
                    drawNeonCross(
                        progress = resultProgress,
                        settle = settle,
                        theme = theme
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNeonSpinner(
    rotation: Float,
    sweep: Float,
    alpha: Float,
    scale: Float,
    theme: PcCompactPaymentVisualTheme
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val spinnerScale = scale.coerceIn(0.92f, 1f)

    val inset = 27.dp.toPx() + (1f - spinnerScale) * 16.dp.toPx()
    val arcTopLeft = Offset(inset, inset)
    val arcSize = Size(
        width = size.width - inset * 2f,
        height = size.height - inset * 2f
    )

    if (theme.showResultRadialGlow) {
        // Очень мягкое внутреннее cyan-свечение, без грубого blob-пятна.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.processingGlowColor.copy(alpha = 0.16f * alpha),
                    theme.processingEdgeColor.copy(alpha = 0.07f * alpha),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension * 0.42f
            ),
            radius = size.minDimension * 0.42f,
            center = center
        )
    }

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
            theme = theme,
            glowColor = theme.processingGlowColor,
            edgeColor = theme.processingEdgeColor,
            coreColor = theme.processingCoreColor
        )
    }
}

private fun DrawScope.drawMorphBridgeStroke(
    progress: Float,
    alpha: Float,
    approved: Boolean,
    settle: Float,
    theme: PcCompactPaymentVisualTheme
) {
    val bridgeProgress = progress.coerceIn(0f, 1f)
    val bridgeAlpha = alpha.coerceIn(0f, 1f)
    if (bridgeProgress <= 0f || bridgeAlpha <= 0f) return

    val start: Offset
    val end: Offset
    val glowColor: Color
    val edgeColor: Color
    val coreColor: Color

    if (approved) {
        start = Offset(size.width * 0.30f, size.height * 0.55f)
        end = Offset(size.width * 0.43f, size.height * 0.67f)
        glowColor = theme.approvedGlowColor
        edgeColor = theme.approvedLineEdgeColor
        coreColor = theme.approvedCoreColor
    } else {
        start = Offset(size.width * 0.32f, size.height * 0.32f)
        end = Offset(size.width * 0.68f, size.height * 0.68f)
        glowColor = theme.declinedGlowColor
        edgeColor = theme.declinedLineEdgeColor
        coreColor = theme.declinedCoreColor
    } 

    drawNeonLine(
        start = start,
        end = end,
        progress = bridgeProgress,
        alpha = bridgeAlpha * 0.82f,
        settle = settle,
        theme = theme,
        glowColor = glowColor,
        edgeColor = edgeColor,
        coreColor = coreColor,
        highlightColor = theme.resultHighlightColor
    )
}

private fun DrawScope.drawNeonCheck(
    progress: Float,
    settle: Float,
    theme: PcCompactPaymentVisualTheme
) {
    val p = progress.coerceIn(0f, 1f)

    val a = Offset(size.width * 0.30f, size.height * 0.55f)
    val b = Offset(size.width * 0.43f, size.height * 0.67f)
    val c = Offset(size.width * 0.72f, size.height * 0.38f)

    val first = neonIntervalProgress(p, 0.00f, 0.42f)
    val second = neonIntervalProgress(p, 0.24f, 1.00f)

    if (theme.showResultRadialGlow) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.approvedGlowColor.copy(alpha = 0.10f * p),
                    theme.approvedLineEdgeColor.copy(alpha = 0.04f * p),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.52f, size.height * 0.52f),
                radius = size.minDimension * 0.34f
            ),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.52f, size.height * 0.52f)
        )
    }

    // Пока галочка рисуется — оставляем по сегментам.
    drawNeonLine(
        start = a,
        end = b,
        progress = first,
        alpha = p,
        settle = settle,
        theme = theme,
        glowColor = theme.approvedGlowColor,
        edgeColor = theme.approvedLineEdgeColor,
        coreColor = theme.approvedCoreColor,
        highlightColor = theme.resultHighlightColor
    )

    drawNeonLine(
        start = b,
        end = c,
        progress = second,
        alpha = p,
        settle = settle,
        theme = theme,
        glowColor = theme.approvedGlowColor,
        edgeColor = theme.approvedLineEdgeColor,
        coreColor = theme.approvedCoreColor,
        highlightColor = theme.resultHighlightColor
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

        if (theme.resultIndicatorStyle == PcCompactResultIndicatorStyle.CleanLight) {
            drawCleanPath(
                path = checkPath,
                alpha = unifiedAlpha,
                settle = settle,
                mainColor = theme.approvedGlowColor,
                softColor = theme.approvedUnifiedEdgeColor,
                coreColor = theme.approvedCoreColor
            )
        } else {
            // Дальнее свечение
            drawPath(
                path = checkPath,
                color = theme.approvedUnifiedEdgeColor.copy(alpha = 0.040f * unifiedAlpha),
                style = Stroke(width = 52.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = checkPath,
                color = theme.approvedGlowColor.copy(alpha = 0.070f * unifiedAlpha),
                style = Stroke(width = 42.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = checkPath,
                color = theme.approvedGlowColor.copy(alpha = 0.145f * unifiedAlpha),
                style = Stroke(width = 32.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = checkPath,
                color = theme.approvedGlowColor.copy(alpha = 0.36f * unifiedAlpha),
                style = Stroke(width = 23.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = checkPath,
                color = theme.approvedCoreColor.copy(alpha = 0.94f * unifiedAlpha),
                style = Stroke(width = 12.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = checkPath,
                color = theme.resultHighlightColor.copy(alpha = 0.20f * unifiedAlpha),
                style = Stroke(width = 5.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

private fun DrawScope.drawNeonCross(
    progress: Float,
    settle: Float,
    theme: PcCompactPaymentVisualTheme
) {
    val p = progress.coerceIn(0f, 1f)

    val a = Offset(size.width * 0.30f, size.height * 0.30f)
    val b = Offset(size.width * 0.70f, size.height * 0.70f)

    val c = Offset(size.width * 0.70f, size.height * 0.30f)
    val d = Offset(size.width * 0.30f, size.height * 0.70f)

    val first = neonIntervalProgress(p, 0.00f, 0.58f)
    val second = neonIntervalProgress(p, 0.24f, 1.00f)

    if (theme.showResultRadialGlow) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.declinedGlowColor.copy(alpha = 0.10f * p),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.50f, size.height * 0.50f),
                radius = size.minDimension * 0.34f
            ),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.50f, size.height * 0.50f)
        )
    }

    drawNeonLine(
        start = a,
        end = b,
        progress = first,
        alpha = p,
        settle = settle,
        theme = theme,
        glowColor = theme.declinedGlowColor,
        edgeColor = theme.declinedLineEdgeColor,
        coreColor = theme.declinedCoreColor,
        highlightColor = theme.resultHighlightColor
    )

    drawNeonLine(
        start = c,
        end = d,
        progress = second,
        alpha = p,
        settle = settle,
        theme = theme,
        glowColor = theme.declinedGlowColor,
        edgeColor = theme.declinedLineEdgeColor,
        coreColor = theme.declinedCoreColor,
        highlightColor = theme.resultHighlightColor
    )

    val unifiedAlpha = ((p - 0.76f) / 0.24f).coerceIn(0f, 1f)

    if (unifiedAlpha > 0f) {
        val crossPath = Path().apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)

            moveTo(c.x, c.y)
            lineTo(d.x, d.y)
        }

        if (theme.resultIndicatorStyle == PcCompactResultIndicatorStyle.CleanLight) {
            drawCleanPath(
                path = crossPath,
                alpha = unifiedAlpha,
                settle = settle,
                mainColor = theme.declinedGlowColor,
                softColor = theme.declinedUnifiedEdgeColor,
                coreColor = theme.declinedCoreColor
            )
        } else {
            drawPath(path = crossPath, color = theme.declinedUnifiedEdgeColor.copy(alpha = 0.040f * unifiedAlpha), style = Stroke(width = 52.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path = crossPath, color = theme.declinedGlowColor.copy(alpha = 0.070f * unifiedAlpha), style = Stroke(width = 42.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path = crossPath, color = theme.declinedGlowColor.copy(alpha = 0.145f * unifiedAlpha), style = Stroke(width = 32.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path = crossPath, color = theme.declinedGlowColor.copy(alpha = 0.36f * unifiedAlpha), style = Stroke(width = 23.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path = crossPath, color = theme.declinedCoreColor.copy(alpha = 0.94f * unifiedAlpha), style = Stroke(width = 12.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path = crossPath, color = theme.resultHighlightColor.copy(alpha = 0.20f * unifiedAlpha), style = Stroke(width = 5.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

}

private fun DrawScope.drawNeonArc(
    topLeft: Offset,
    size: Size,
    startAngle: Float,
    sweepAngle: Float,
    alpha: Float,
    theme: PcCompactPaymentVisualTheme,
    glowColor: Color,
    edgeColor: Color,
    coreColor: Color
) {
    if (theme.resultIndicatorStyle == PcCompactResultIndicatorStyle.CleanLight) {
        drawCleanArc(topLeft, size, startAngle, sweepAngle, alpha, glowColor, edgeColor, coreColor)
        return
    }
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
    theme: PcCompactPaymentVisualTheme,
    glowColor: Color,
    edgeColor: Color,
    coreColor: Color,
    highlightColor: Color = Color.White
) {
    if (theme.resultIndicatorStyle == PcCompactResultIndicatorStyle.CleanLight) {
        drawCleanLine(start, end, progress, alpha, settle, glowColor, edgeColor, coreColor)
        return
    }
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
        color = highlightColor.copy(alpha = 0.20f * alpha),
        start = start,
        end = currentEnd,
        strokeWidth = 5.dp.toPx() * settle,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCleanArc(
    topLeft: Offset,
    size: Size,
    startAngle: Float,
    sweepAngle: Float,
    alpha: Float,
    mainColor: Color,
    softColor: Color,
    coreColor: Color
) {
    drawArc(color = softColor.copy(alpha = 0.10f * alpha), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = topLeft, size = size, style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
    drawArc(color = softColor.copy(alpha = 0.18f * alpha), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = topLeft, size = size, style = Stroke(width = 17.dp.toPx(), cap = StrokeCap.Round))
    drawArc(color = mainColor.copy(alpha = 0.92f * alpha), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = topLeft, size = size, style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round))
    drawArc(color = coreColor.copy(alpha = 0.35f * alpha), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = topLeft, size = size, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawCleanLine(
    start: Offset,
    end: Offset,
    progress: Float,
    alpha: Float,
    settle: Float,
    mainColor: Color,
    softColor: Color,
    coreColor: Color
) {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return
    val currentEnd = Offset(x = start.x + (end.x - start.x) * p, y = start.y + (end.y - start.y) * p)
    drawLine(color = softColor.copy(alpha = 0.10f * alpha), start = start, end = currentEnd, strokeWidth = 34.dp.toPx() * settle, cap = StrokeCap.Round)
    drawLine(color = softColor.copy(alpha = 0.18f * alpha), start = start, end = currentEnd, strokeWidth = 22.dp.toPx() * settle, cap = StrokeCap.Round)
    drawLine(color = mainColor.copy(alpha = 0.95f * alpha), start = start, end = currentEnd, strokeWidth = 12.dp.toPx() * settle, cap = StrokeCap.Round)
    drawLine(color = coreColor.copy(alpha = 0.28f * alpha), start = start, end = currentEnd, strokeWidth = 5.dp.toPx() * settle, cap = StrokeCap.Round)
}

private fun DrawScope.drawCleanPath(
    path: Path,
    alpha: Float,
    settle: Float,
    mainColor: Color,
    softColor: Color,
    coreColor: Color
) {
    drawPath(path = path, color = softColor.copy(alpha = 0.10f * alpha), style = Stroke(width = 34.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = path, color = softColor.copy(alpha = 0.18f * alpha), style = Stroke(width = 22.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = path, color = mainColor.copy(alpha = 0.95f * alpha), style = Stroke(width = 12.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = path, color = coreColor.copy(alpha = 0.28f * alpha), style = Stroke(width = 5.dp.toPx() * settle, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun neonIntervalProgress(
    progress: Float,
    start: Float,
    end: Float
): Float {
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private fun Color.withMultipliedAlpha(multiplier: Float): Color =
    copy(alpha = alpha * multiplier.coerceIn(0f, 1f))

@Composable
private fun PcCompactTopRings(alpha: Float = 1f, theme: PcCompactPaymentVisualTheme) {
    if (!theme.showTopRings) return
    val ringAlpha = alpha.coerceIn(0f, 1f)
    if (ringAlpha <= 0f) return

    val transition = rememberInfiniteTransition(label = "top_rings_premium")

    val primaryPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "top_rings_primary_phase"
    )

    val secondaryPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2900,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "top_rings_secondary_phase"
    )

    val backgroundPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4700,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "top_rings_background_phase"
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

        val ringColor = theme.topRingColor
        val glowColor = theme.topRingGlowColor

        fun smoothWave(progress: Float): Float {
            val p = progress.coerceIn(0f, 1f)
            return 0.5f - 0.5f * cos((p * 2f * PI).toFloat())
        }

        fun ringProgress(phase: Float, duration: Float): Float? {
            val wrappedPhase = phase % 1f

            return if (wrappedPhase <= duration) {
                wrappedPhase / duration
            } else {
                null
            }
        }

        fun drawPulseRing(phase: Float, radiusDp: Float, maxAlpha: Float, duration: Float) {
            val progress = ringProgress(phase = phase, duration = duration)

            if (progress != null) {
                val wave = smoothWave(progress)

                val radius = radiusDp.dp.toPx() + wave * 12.dp.toPx()
                val waveAlpha = wave * maxAlpha
                val glowAlpha = wave * maxAlpha * 0.40f

                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha * ringAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawCircle(
                    color = ringColor.copy(alpha = waveAlpha * ringAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 1.8.dp.toPx() + wave * 0.75.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawCircle(
                    color = Color.White.copy(alpha = waveAlpha * 0.32f * ringAlpha),
                    radius = radius - 1.5.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 0.7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        drawPulseRing(
            phase = primaryPhase.value,
            radiusDp = 54f,
            maxAlpha = 0.28f,
            duration = 0.40f
        )
        drawPulseRing(
            phase = (secondaryPhase.value + 0.33f) % 1f,
            radiusDp = 86f,
            maxAlpha = 0.22f,
            duration = 0.38f
        )
        drawPulseRing(
            phase = (backgroundPhase.value + 0.66f) % 1f,
            radiusDp = 122f,
            maxAlpha = 0.17f,
            duration = 0.42f
        )
        drawPulseRing(
            phase = (primaryPhase.value + 0.18f) % 1f,
            radiusDp = 162f,
            maxAlpha = 0.12f,
            duration = 0.36f
        )

        val ambient = 0.5f - 0.5f * cos((backgroundPhase.value * 2f * PI).toFloat())

        drawCircle(
            color = glowColor.copy(alpha = (0.032f + ambient * 0.020f) * ringAlpha),
            radius = 118.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun PcCompactDecorativeBankCard(
    theme: PcCompactPaymentVisualTheme,
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
        painter = painterResource(id = theme.decorativeCardDrawable ?: R.drawable.pc_compact_bank_card),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(
                width = theme.decorativeCardWidth,
                height = theme.decorativeCardHeight
            )
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
    theme: PcCompactPaymentVisualTheme,
    modifier: Modifier,
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val rowAlpha = if (enabled) 1f else 0.5f
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(theme.serviceFeeRowBrush(rowAlpha))
            .border(
                width = 1.dp,
                color = theme.serviceFeeBorderColor.withMultipliedAlpha(rowAlpha),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = theme.serviceFeeTextColor.withMultipliedAlpha(0.92f * rowAlpha),
            fontSize = 14.sp,
            fontFamily = MontserratFontFamily
        )

        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (checked) theme.serviceFeeToggleOnBrush else theme.serviceFeeToggleOffBrush
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
    theme: PcCompactPaymentVisualTheme,
    percentText: String,
    amountText: String? = null,
    selected: Boolean,
    enabled: Boolean,
    visuallyEnabled: Boolean = enabled,
    amountFontSize: TextUnit? = null,
    cardWidth: Dp = PC_COMPACT_TIP_CARD_MIN_WIDTH,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val visualAlpha = if (visuallyEnabled) 1f else 0.5f

    val backgroundBrush = if (selected) theme.selectedTipBrush(visualAlpha) else theme.unselectedTipBrush(visualAlpha)

    val showSelectedBlurGlow = selected && theme.selectedTipBlurGlowEnabled
    val cardBrush = if (showSelectedBlurGlow) theme.selectedTipGlassBrush(visualAlpha) else backgroundBrush
    val borderColor = when {
        showSelectedBlurGlow -> Color.White.copy(alpha = 0.46f * visualAlpha)
        selected -> theme.selectedBorderColor.withMultipliedAlpha(visualAlpha)
        else -> theme.unselectedBorderColor.withMultipliedAlpha(visualAlpha)
    }

    Box(
        modifier = Modifier.size(width = cardWidth, height = PC_COMPACT_TIP_CARD_HEIGHT)
    ) {
        if (showSelectedBlurGlow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .offset(y = 3.dp)
                    .blur(16.dp)
                    .clip(shape)
                    .background(theme.selectedTipBlurGlowBrush(visualAlpha))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(cardBrush)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = shape
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = percentText,
                    color = (if (selected) Color.White else theme.primaryTextColor).copy(alpha = visualAlpha),
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFontFamily,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!amountText.isNullOrBlank()) {
                    Text(
                        text = amountText,
                        color = Color.White.copy(alpha = 0.82f * visualAlpha),
                        fontSize = amountFontSize ?: 14.sp,
                        fontFamily = MontserratFontFamily,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PcCompactNoTipsButton(
    theme: PcCompactPaymentVisualTheme,
    selected: Boolean,
    enabled: Boolean,
    visuallyEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val visualAlpha = if (visuallyEnabled) 1f else 0.5f
    val backgroundBrush = if (selected) theme.noTipsSelectedBrush(visualAlpha) else theme.noTipsUnselectedBrush(visualAlpha)
    Box(
        modifier = modifier
            .size(width = 448.dp, height = 56.dp)
            .clip(shape)
            .background(backgroundBrush)
            .border(
                1.dp,
                if (selected) {
                    theme.noTipsSelectedBorderColor.withMultipliedAlpha(visualAlpha)
                } else {
                    theme.noTipsUnselectedBorderColor.withMultipliedAlpha(visualAlpha)
                },
                shape
            )
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Без чаевых",
            color = (if (selected) Color.White else theme.primaryTextColor).copy(alpha = visualAlpha),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFontFamily
        )
    }
}



private data class PcCompactPaymentVisualTheme(
    val backgroundBrush: Brush,
    val errorBackgroundBrush: Brush? = null,
    val useAnimatedDefaultBackground: Boolean = true,
    val glowColor: Color,
    val primaryTextColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val topRingColor: Color,
    val topRingGlowColor: Color,
    val selectedTipBrush: (Float) -> Brush,
    val selectedTipBlurGlowEnabled: Boolean = false,
    val selectedTipBlurGlowBrush: (Float) -> Brush = { alpha -> selectedTipBrush(alpha) },
    val selectedTipGlassBrush: (Float) -> Brush = { alpha -> selectedTipBrush(alpha) },
    val unselectedTipBrush: (Float) -> Brush,
    val noTipsSelectedBrush: (Float) -> Brush,
    val noTipsUnselectedBrush: (Float) -> Brush,
    val serviceFeeRowBrush: (Float) -> Brush,
    val serviceFeeToggleOnBrush: Brush,
    val serviceFeeToggleOffBrush: Brush,
    val serviceFeeTextColor: Color,
    val serviceFeeBorderColor: Color,
    val selectedBorderColor: Color,
    val unselectedBorderColor: Color,
    val noTipsSelectedBorderColor: Color,
    val noTipsUnselectedBorderColor: Color,
    val approvedColor: Color,
    val declinedColor: Color,
    val processingColor: Color,
    val processingCoreColor: Color,
    val processingGlowColor: Color,
    val processingEdgeColor: Color,
    val approvedCoreColor: Color,
    val approvedGlowColor: Color,
    val approvedLineEdgeColor: Color,
    val approvedUnifiedEdgeColor: Color,
    val declinedCoreColor: Color,
    val declinedGlowColor: Color,
    val declinedLineEdgeColor: Color,
    val declinedUnifiedEdgeColor: Color,
    val resultHighlightColor: Color,
    val resultIndicatorStyle: PcCompactResultIndicatorStyle,
    val showResultRadialGlow: Boolean,
    val showTopRings: Boolean,
    val closeIconDrawable: Int,
    val closeIconTint: Color,
    val decorativeCardDrawable: Int? = null,
    val decorativeCardWidth: Dp = 190.dp,
    val decorativeCardHeight: Dp = 122.dp
)

@Composable
private fun rememberPcCompactPaymentVisualTheme(style: PcCompactPaymentDesignStyle): PcCompactPaymentVisualTheme = when (style) {
    PcCompactPaymentDesignStyle.DEFAULT -> defaultPcCompactPaymentTheme()
    PcCompactPaymentDesignStyle.ALFA -> alfaPcCompactPaymentTheme()
}

private fun defaultPcCompactPaymentTheme() = PcCompactPaymentVisualTheme(
    backgroundBrush = Brush.linearGradient(listOf(Color(0xFF151D25), Color(0xFF0E5C91), Color(0xFF1B222A))),
    useAnimatedDefaultBackground = true,
    glowColor = Color(0xFF126CA4),
    primaryTextColor = Color.White,
    secondaryTextColor = Color.White.copy(alpha = 0.78f),
    closeIconDrawable = R.drawable.ic_payment_close,
    closeIconTint = Color.Unspecified,
    accentColor = Color(0xFF20D6D2),
    topRingColor = Color(0xFF7DE8FF),
    topRingGlowColor = Color(0xFF20D6D2),
    selectedTipBrush = { alpha -> Brush.verticalGradient(listOf(Color(0xFF74E8E1).copy(alpha = alpha), Color(0xFF20B8C8).copy(alpha = alpha))) },
    selectedTipBlurGlowEnabled = true,
    selectedTipBlurGlowBrush = { alpha ->
        Brush.verticalGradient(
            listOf(
                Color(0xFF74E8E1).copy(alpha = alpha),
                Color(0xFF20B8C8).copy(alpha = alpha),
                Color(0xFF126CA4).copy(alpha = alpha)
            )
        )
    },
    selectedTipGlassBrush = { alpha ->
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.26f * alpha),
                Color(0xFFE7FFFF).copy(alpha = 0.16f * alpha),
                Color.White.copy(alpha = 0.12f * alpha)
            )
        )
    },
    unselectedTipBrush = { alpha -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f * alpha), Color.White.copy(alpha = 0.12f * alpha))) },
    noTipsSelectedBrush = { alpha -> Brush.verticalGradient(listOf(Color(0xFF74E8E1).copy(alpha = alpha), Color(0xFF20B8C8).copy(alpha = alpha))) },
    noTipsUnselectedBrush = { alpha -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f * alpha), Color.White.copy(alpha = 0.10f * alpha))) },
    serviceFeeRowBrush = { alpha -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f * alpha), Color.White.copy(alpha = 0.20f * alpha))) },
    serviceFeeToggleOnBrush = Brush.horizontalGradient(listOf(Color(0xFF20D6D2), Color(0xFF126CA4))),
    serviceFeeToggleOffBrush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.18f))),
    serviceFeeTextColor = Color.White,
    serviceFeeBorderColor = Color.White.copy(alpha = 0.25f),
    selectedBorderColor = Color.White.copy(alpha = 0.48f),
    unselectedBorderColor = Color.White.copy(alpha = 0.34f),
    noTipsSelectedBorderColor = Color.White.copy(alpha = 0.45f),
    noTipsUnselectedBorderColor = Color.White.copy(alpha = 0.30f),
    approvedColor = Color(0xFF19F1D4), declinedColor = Color(0xFFFF3030), processingColor = Color(0xFF16E8D3),
    processingCoreColor = Color(0xFFCFFFF8),
    processingGlowColor = Color(0xFF16E8D3),
    processingEdgeColor = Color(0xFF20D6D2),
    approvedCoreColor = Color(0xFFCFFFF8),
    approvedGlowColor = Color(0xFF19F1D4),
    approvedLineEdgeColor = Color(0xFF20D6D2),
    approvedUnifiedEdgeColor = Color(0xFF118BD7),
    declinedCoreColor = Color(0xFFFFA0A0),
    declinedGlowColor = Color(0xFFFF3030),
    declinedLineEdgeColor = Color(0xFFFF3030),
    declinedUnifiedEdgeColor = Color(0xFFFF1F1F),
    resultHighlightColor = Color.White,
    resultIndicatorStyle = PcCompactResultIndicatorStyle.NeonDark,
    showResultRadialGlow = true,
    showTopRings = true,
    decorativeCardWidth = 260.dp,
    decorativeCardHeight = 166.dp
)

private val AlfaBg = Color.White
private val AlfaText = Color(0xFF121820)
private val AlfaSubText = Color(0xFF2B3037)
private val AlfaRed = Color(0xFFE31B23)
private val AlfaRedDark = Color(0xFFC81720)
private val AlfaPaleBlue = Color(0xFFE8F2FF)
private val AlfaGreen = Color(0xFF23B26D)
private val AlfaDeclineRed = Color(0xFFE94545)

// TODO: After adding app/src/main/res/drawable-nodpi/pc_alt_bank_card.png, replace with R.drawable.pc_alt_bank_card.
private val AlfaBankCardDrawable = R.drawable.pc_alt_bank_card

private fun alfaPcCompactPaymentTheme() = PcCompactPaymentVisualTheme(
    backgroundBrush = Brush.verticalGradient(
        listOf(
            Color.White,
            Color.White
        )
    ),
    errorBackgroundBrush = Brush.verticalGradient(
        listOf(
            Color.White,
            Color.White
        )
    ),
    useAnimatedDefaultBackground = false,
    glowColor = Color.White,
    primaryTextColor = AlfaText,
    secondaryTextColor = AlfaSubText,
    accentColor = AlfaRed,
    topRingColor = AlfaRed.copy(alpha = 0.42f),
    topRingGlowColor = AlfaRed.copy(alpha = 0.18f),
    closeIconDrawable = R.drawable.ic_alfa_payment_close,
    closeIconTint = Color.Unspecified,
    selectedTipBrush = { alpha -> Brush.verticalGradient(listOf(AlfaRed.copy(alpha = alpha), AlfaRedDark.copy(alpha = alpha))) },
    unselectedTipBrush = { alpha -> Brush.verticalGradient(listOf(AlfaPaleBlue.copy(alpha = alpha), AlfaPaleBlue.copy(alpha = alpha))) },
    noTipsSelectedBrush = { alpha -> Brush.verticalGradient(listOf(AlfaRed.copy(alpha = alpha), AlfaRedDark.copy(alpha = alpha))) },
    noTipsUnselectedBrush = { alpha -> Brush.verticalGradient(listOf(AlfaPaleBlue.copy(alpha = alpha), AlfaPaleBlue.copy(alpha = alpha))) },
    serviceFeeRowBrush = { alpha -> Brush.verticalGradient(listOf(AlfaPaleBlue.copy(alpha = alpha), AlfaPaleBlue.copy(alpha = alpha))) },
    serviceFeeToggleOnBrush = Brush.horizontalGradient(listOf(AlfaRed, AlfaRedDark)),
    serviceFeeToggleOffBrush = Brush.horizontalGradient(listOf(AlfaPaleBlue, AlfaPaleBlue)),
    serviceFeeTextColor = AlfaText,
    serviceFeeBorderColor = Color(0xFFD7E5F7),
    selectedBorderColor = AlfaRed.copy(alpha = 0.24f),
    unselectedBorderColor = Color(0xFFD7E5F7),
    noTipsSelectedBorderColor = AlfaRed.copy(alpha = 0.24f),
    noTipsUnselectedBorderColor = Color(0xFFD7E5F7),
    approvedColor = AlfaGreen,
    declinedColor = AlfaDeclineRed,
    processingColor = AlfaGreen,
    processingCoreColor = Color.White,
    processingGlowColor = Color(0xFF24B96F),
    processingEdgeColor = Color(0xFF24B96F),
    approvedCoreColor = Color.White,
    approvedGlowColor = Color(0xFF24B96F),
    approvedLineEdgeColor = Color(0xFF24B96F),
    approvedUnifiedEdgeColor = Color(0xFF24B96F),
    declinedCoreColor = Color.White,
    declinedGlowColor = Color(0xFFE53935),
    declinedLineEdgeColor = Color(0xFFE53935),
    declinedUnifiedEdgeColor = Color(0xFFE53935),
    resultHighlightColor = Color.White,
    resultIndicatorStyle = PcCompactResultIndicatorStyle.CleanLight,
    showResultRadialGlow = false,
    showTopRings = true,
    decorativeCardDrawable = AlfaBankCardDrawable,
    decorativeCardWidth = 260.dp,
    decorativeCardHeight = 166.dp
)

package com.chaiok.pos.presentation.cardpresenting

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val CardPresentingBackgroundColor = Color.White
private val CardPresentingPrimaryTextColor = Color(0xFF1B2128)
private val CardPresentingSecondaryTextColor = Color(0xFF69707A)
private val CardPresentingAccentColor = Color(0xFF087BE8)
private val CardPresentingGreenColor = Color(0xFF14B8A6)
private val CardPresentingErrorColor = Color(0xFFFF5A5F)
private val CardPresentingSoftCardColor = Color(0xFFF7F8FA)
private val CardPresentingStrokeColor = Color(0xFFE2E7EF)

private data class CardPresentingLayoutMetrics(
    val isSquareCompact: Boolean,

    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,

    val headerHeight: Dp,
    val logoWidth: Dp,
    val logoHeight: Dp,

    val headerToAmountSpacing: Dp,
    val amountToMainSpacing: Dp,
    val mainToButtonSpacing: Dp,

    val amountCardCornerRadius: Dp,
    val amountCardShadowElevation: Dp,
    val amountCardHorizontalPadding: Dp,
    val amountCardVerticalPadding: Dp,
    val amountLabelFontSize: TextUnit,
    val amountLabelLineHeight: TextUnit,
    val amountFontSize: TextUnit,
    val amountLineHeight: TextUnit,
    val amountTextSpacing: Dp,

    val mainCardCornerRadius: Dp,
    val mainCardShadowElevation: Dp,
    val mainCardHorizontalPadding: Dp,
    val mainCardVerticalPadding: Dp,
    val visualSize: Dp,
    val visualPulseBaseSize: Dp,
    val visualWavesSize: Dp,
    val visualIconBoxSize: Dp,
    val visualIconBoxCornerRadius: Dp,
    val visualIconBoxShadowElevation: Dp,
    val visualIconSize: Dp,
    val nfcWaveStrokeWidth: Dp,
    val nfcWaveRadii: List<Dp>,

    val visualToTitleSpacing: Dp,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val titleMaxLines: Int,

    val titleToMessageSpacing: Dp,
    val messageFontSize: TextUnit,
    val messageLineHeight: TextUnit,
    val messageMaxLines: Int,

    val loadingSpacing: Dp,
    val loadingSize: Dp,
    val loadingStrokeWidth: Dp,

    val cancelButtonHeight: Dp,
    val cancelButtonCornerRadius: Dp,
    val cancelButtonShadowElevation: Dp,
    val cancelButtonFontSize: TextUnit,
    val cancelButtonLineHeight: TextUnit
)

@Composable
private fun cardPresentingLayoutMetrics(): CardPresentingLayoutMetrics {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp

    return if (isSquareCompact) {
        CardPresentingLayoutMetrics(
            isSquareCompact = true,

            horizontalPadding = 14.dp,
            topPadding = 8.dp,
            bottomPadding = 10.dp,

            headerHeight = 42.dp,
            logoWidth = 78.dp,
            logoHeight = 26.dp,

            headerToAmountSpacing = 8.dp,
            amountToMainSpacing = 8.dp,
            mainToButtonSpacing = 8.dp,

            amountCardCornerRadius = 22.dp,
            amountCardShadowElevation = 4.dp,
            amountCardHorizontalPadding = 14.dp,
            amountCardVerticalPadding = 9.dp,
            amountLabelFontSize = 11.sp,
            amountLabelLineHeight = 14.sp,
            amountFontSize = 24.sp,
            amountLineHeight = 28.sp,
            amountTextSpacing = 2.dp,

            mainCardCornerRadius = 26.dp,
            mainCardShadowElevation = 6.dp,
            mainCardHorizontalPadding = 14.dp,
            mainCardVerticalPadding = 12.dp,
            visualSize = 120.dp,
            visualPulseBaseSize = 92.dp,
            visualWavesSize = 116.dp,
            visualIconBoxSize = 76.dp,
            visualIconBoxCornerRadius = 26.dp,
            visualIconBoxShadowElevation = 8.dp,
            visualIconSize = 36.dp,
            nfcWaveStrokeWidth = 3.dp,
            nfcWaveRadii = listOf(32.dp, 43.dp, 54.dp),

            visualToTitleSpacing = 8.dp,
            titleFontSize = 18.sp,
            titleLineHeight = 22.sp,
            titleMaxLines = 2,

            titleToMessageSpacing = 4.dp,
            messageFontSize = 12.sp,
            messageLineHeight = 16.sp,
            messageMaxLines = 2,

            loadingSpacing = 6.dp,
            loadingSize = 22.dp,
            loadingStrokeWidth = 2.dp,

            cancelButtonHeight = 44.dp,
            cancelButtonCornerRadius = 18.dp,
            cancelButtonShadowElevation = 4.dp,
            cancelButtonFontSize = 14.sp,
            cancelButtonLineHeight = 17.sp
        )
    } else {
        CardPresentingLayoutMetrics(
            isSquareCompact = false,

            horizontalPadding = 24.dp,
            topPadding = 18.dp,
            bottomPadding = 24.dp,

            headerHeight = 64.dp,
            logoWidth = 104.dp,
            logoHeight = 36.dp,

            headerToAmountSpacing = 28.dp,
            amountToMainSpacing = 24.dp,
            mainToButtonSpacing = 22.dp,

            amountCardCornerRadius = 28.dp,
            amountCardShadowElevation = 8.dp,
            amountCardHorizontalPadding = 20.dp,
            amountCardVerticalPadding = 18.dp,
            amountLabelFontSize = 14.sp,
            amountLabelLineHeight = 18.sp,
            amountFontSize = 34.sp,
            amountLineHeight = 40.sp,
            amountTextSpacing = 6.dp,

            mainCardCornerRadius = 34.dp,
            mainCardShadowElevation = 12.dp,
            mainCardHorizontalPadding = 22.dp,
            mainCardVerticalPadding = 26.dp,
            visualSize = 190.dp,
            visualPulseBaseSize = 150.dp,
            visualWavesSize = 178.dp,
            visualIconBoxSize = 118.dp,
            visualIconBoxCornerRadius = 38.dp,
            visualIconBoxShadowElevation = 14.dp,
            visualIconSize = 54.dp,
            nfcWaveStrokeWidth = 4.dp,
            nfcWaveRadii = listOf(52.dp, 72.dp, 92.dp),

            visualToTitleSpacing = 26.dp,
            titleFontSize = 24.sp,
            titleLineHeight = 30.sp,
            titleMaxLines = 2,

            titleToMessageSpacing = 10.dp,
            messageFontSize = 15.sp,
            messageLineHeight = 21.sp,
            messageMaxLines = 3,

            loadingSpacing = 22.dp,
            loadingSize = 34.dp,
            loadingStrokeWidth = 3.dp,

            cancelButtonHeight = 56.dp,
            cancelButtonCornerRadius = 24.dp,
            cancelButtonShadowElevation = 8.dp,
            cancelButtonFontSize = 16.sp,
            cancelButtonLineHeight = 20.sp
        )
    }
}

@Composable
fun CardPresentingScreen(
    state: CardPresentingUiState,
    onCancel: () -> Unit
) {
    val metrics = cardPresentingLayoutMetrics()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardPresentingBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.horizontalPadding)
                .padding(top = metrics.topPadding, bottom = metrics.bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PaymentHeader(metrics = metrics)

            Spacer(modifier = Modifier.height(metrics.headerToAmountSpacing))

            AmountCard(
                amountText = state.amountText,
                metrics = metrics
            )

            Spacer(modifier = Modifier.height(metrics.amountToMainSpacing))

            CardPresentingMainCard(
                state = state,
                metrics = metrics,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(metrics.mainToButtonSpacing))

            CancelPaymentButton(
                enabled = state.canCancel,
                metrics = metrics,
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun PaymentHeader(
    metrics: CardPresentingLayoutMetrics
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.headerHeight),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.tiply_logo_black),
            contentDescription = "Tiply",
            modifier = Modifier.size(
                width = metrics.logoWidth,
                height = metrics.logoHeight
            ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AmountCard(
    amountText: String,
    metrics: CardPresentingLayoutMetrics
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.amountCardShadowElevation,
                shape = RoundedCornerShape(metrics.amountCardCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(metrics.amountCardCornerRadius))
            .background(CardPresentingSoftCardColor)
            .border(
                width = 1.dp,
                color = CardPresentingStrokeColor,
                shape = RoundedCornerShape(metrics.amountCardCornerRadius)
            )
            .padding(
                horizontal = metrics.amountCardHorizontalPadding,
                vertical = metrics.amountCardVerticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сумма к оплате",
            color = CardPresentingSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.amountLabelFontSize,
            lineHeight = metrics.amountLabelLineHeight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(metrics.amountTextSpacing))

        Text(
            text = amountText.ifBlank { "₽" },
            color = CardPresentingPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.amountFontSize,
            lineHeight = metrics.amountLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CardPresentingMainCard(
    state: CardPresentingUiState,
    metrics: CardPresentingLayoutMetrics,
    modifier: Modifier = Modifier
) {
    val accentBrush = when (state.stage) {
        CardPresentingStage.Approved -> {
            Brush.linearGradient(
                listOf(
                    CardPresentingGreenColor,
                    CardPresentingAccentColor
                )
            )
        }

        CardPresentingStage.Declined,
        CardPresentingStage.Error,
        CardPresentingStage.Cancelled -> {
            Brush.linearGradient(
                listOf(
                    CardPresentingErrorColor,
                    Color(0xFFFF8E8E)
                )
            )
        }

        else -> {
            Brush.linearGradient(
                listOf(
                    CardPresentingAccentColor,
                    CardPresentingGreenColor
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.mainCardShadowElevation,
                shape = RoundedCornerShape(metrics.mainCardCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(metrics.mainCardCornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF3F7FB)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = CardPresentingStrokeColor,
                shape = RoundedCornerShape(metrics.mainCardCornerRadius)
            )
            .padding(
                horizontal = metrics.mainCardHorizontalPadding,
                vertical = metrics.mainCardVerticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedPaymentVisual(
            stage = state.stage,
            accentBrush = accentBrush,
            isLoading = state.isLoading,
            metrics = metrics
        )

        Spacer(modifier = Modifier.height(metrics.visualToTitleSpacing))

        Text(
            text = state.title,
            color = CardPresentingPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.titleFontSize,
            lineHeight = metrics.titleLineHeight,
            textAlign = TextAlign.Center,
            maxLines = metrics.titleMaxLines,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(metrics.titleToMessageSpacing))

        Text(
            text = state.message,
            color = CardPresentingSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.messageFontSize,
            lineHeight = metrics.messageLineHeight,
            textAlign = TextAlign.Center,
            maxLines = metrics.messageMaxLines,
            overflow = TextOverflow.Ellipsis
        )

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(metrics.loadingSpacing))

            CircularProgressIndicator(
                modifier = Modifier.size(metrics.loadingSize),
                color = CardPresentingAccentColor,
                strokeWidth = metrics.loadingStrokeWidth
            )
        }
    }
}

@Composable
private fun AnimatedPaymentVisual(
    stage: CardPresentingStage,
    accentBrush: Brush,
    isLoading: Boolean,
    metrics: CardPresentingLayoutMetrics
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_presenting_animation")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = when (stage) {
            CardPresentingStage.Approved,
            CardPresentingStage.Declined,
            CardPresentingStage.Error,
            CardPresentingStage.Cancelled -> 1.08f

            else -> 1f
        },
        animationSpec = tween(
            durationMillis = 260,
            easing = FastOutSlowInEasing
        ),
        label = "iconScale"
    )

    Box(
        modifier = Modifier.size(metrics.visualSize),
        contentAlignment = Alignment.Center
    ) {
        if (stage.shouldShowNfcPulse()) {
            Box(
                modifier = Modifier
                    .size((metrics.visualPulseBaseSize.value * pulseScale).dp)
                    .clip(CircleShape)
                    .background(
                        CardPresentingAccentColor.copy(alpha = pulseAlpha)
                    )
            )

            NfcWaves(
                modifier = Modifier.size(metrics.visualWavesSize),
                alpha = pulseAlpha,
                strokeWidth = metrics.nfcWaveStrokeWidth,
                waveRadii = metrics.nfcWaveRadii
            )
        }

        Box(
            modifier = Modifier
                .size(metrics.visualIconBoxSize)
                .shadow(
                    elevation = metrics.visualIconBoxShadowElevation,
                    shape = RoundedCornerShape(metrics.visualIconBoxCornerRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.20f)
                )
                .clip(RoundedCornerShape(metrics.visualIconBoxCornerRadius))
                .background(accentBrush),
            contentAlignment = Alignment.Center
        ) {
            StageIcon(
                stage = stage,
                isLoading = isLoading,
                modifier = Modifier.size((metrics.visualIconSize.value * iconScale).dp)
            )
        }
    }
}

@Composable
private fun NfcWaves(
    modifier: Modifier = Modifier,
    alpha: Float,
    strokeWidth: Dp,
    waveRadii: List<Dp>
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )

        val center = Offset(size.width / 2f, size.height / 2f)

        waveRadii.forEachIndexed { index, radiusDp ->
            val radius = radiusDp.toPx()
            val animatedAlpha = (alpha - index * 0.07f).coerceIn(0f, 1f)

            drawArc(
                color = CardPresentingAccentColor.copy(alpha = animatedAlpha),
                startAngle = -46f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = stroke
            )

            drawArc(
                color = CardPresentingGreenColor.copy(alpha = animatedAlpha),
                startAngle = 134f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = stroke
            )
        }
    }
}

@Composable
private fun StageIcon(
    stage: CardPresentingStage,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val imageVector = when (stage) {
        CardPresentingStage.Approved -> Icons.Default.Check
        CardPresentingStage.Declined,
        CardPresentingStage.Error,
        CardPresentingStage.Cancelled -> Icons.Default.Close
        CardPresentingStage.PinRequired -> Icons.Default.Lock
        else -> Icons.Default.CreditCard
    }

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier
    )
}

@Composable
private fun CancelPaymentButton(
    enabled: Boolean,
    metrics: CardPresentingLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.cancelButtonHeight)
            .shadow(
                elevation = if (enabled) metrics.cancelButtonShadowElevation else 0.dp,
                shape = RoundedCornerShape(metrics.cancelButtonCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (enabled) 0.08f else 0f),
                spotColor = Color.Black.copy(alpha = if (enabled) 0.14f else 0f)
            )
            .clip(RoundedCornerShape(metrics.cancelButtonCornerRadius))
            .background(
                brush = if (enabled) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFFFF8E8E)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFE2E7EF),
                            Color(0xFFE2E7EF)
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
            text = if (enabled) "Отменить оплату" else "Отмена недоступна",
            color = if (enabled) Color.White else CardPresentingSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.cancelButtonFontSize,
            lineHeight = metrics.cancelButtonLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun CardPresentingStage.shouldShowNfcPulse(): Boolean {
    return when (this) {
        CardPresentingStage.Idle,
        CardPresentingStage.Preparing,
        CardPresentingStage.WaitingForCard,
        CardPresentingStage.CardDetected -> true

        CardPresentingStage.Processing,
        CardPresentingStage.PinRequired,
        CardPresentingStage.Approved,
        CardPresentingStage.Declined,
        CardPresentingStage.Error,
        CardPresentingStage.Cancelling,
        CardPresentingStage.Cancelled -> false
    }
}
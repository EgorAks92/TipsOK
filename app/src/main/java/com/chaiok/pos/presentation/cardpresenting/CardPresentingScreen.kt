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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
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
    val visualIconBoxIsCircle: Boolean,
    val showPulseCircle: Boolean,
    val useProcessingSpinner: Boolean,
    val spinnerSize: Dp,
    val spinnerStrokeWidth: Dp,

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

private fun regularCardPresentingMetrics(): CardPresentingLayoutMetrics {
    return CardPresentingLayoutMetrics(
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
        visualIconBoxIsCircle = false,
        showPulseCircle = true,
        useProcessingSpinner = false,
        spinnerSize = 34.dp,
        spinnerStrokeWidth = 3.dp,

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

private fun squarePremiumCardPresentingMetrics(): CardPresentingLayoutMetrics {
    return CardPresentingLayoutMetrics(
        horizontalPadding = 18.dp,
        topPadding = 8.dp,
        bottomPadding = 10.dp,

        headerHeight = 30.dp,
        logoWidth = 82.dp,
        logoHeight = 26.dp,

        headerToAmountSpacing = 8.dp,
        amountToMainSpacing = 8.dp,
        mainToButtonSpacing = 8.dp,

        amountCardCornerRadius = 24.dp,
        amountCardShadowElevation = 5.dp,
        amountCardHorizontalPadding = 18.dp,
        amountCardVerticalPadding = 7.dp,
        amountLabelFontSize = 9.sp,
        amountLabelLineHeight = 12.sp,
        amountFontSize = 27.sp,
        amountLineHeight = 31.sp,
        amountTextSpacing = 0.dp,

        mainCardCornerRadius = 0.dp,
        mainCardShadowElevation = 0.dp,
        mainCardHorizontalPadding = 0.dp,
        mainCardVerticalPadding = 0.dp,

        visualSize = 176.dp,
        visualPulseBaseSize = 132.dp,
        visualWavesSize = 174.dp,
        visualIconBoxSize = 108.dp,
        visualIconBoxCornerRadius = 54.dp,
        visualIconBoxShadowElevation = 15.dp,
        visualIconSize = 50.dp,
        visualIconBoxIsCircle = true,
        showPulseCircle = false,
        useProcessingSpinner = true,
        spinnerSize = 42.dp,
        spinnerStrokeWidth = 4.dp,

        nfcWaveStrokeWidth = 3.dp,
        nfcWaveRadii = listOf(46.dp, 64.dp, 82.dp),

        visualToTitleSpacing = 9.dp,
        titleFontSize = 21.sp,
        titleLineHeight = 24.sp,
        titleMaxLines = 2,

        titleToMessageSpacing = 4.dp,
        messageFontSize = 12.sp,
        messageLineHeight = 16.sp,
        messageMaxLines = 2,

        loadingSpacing = 8.dp,
        loadingSize = 22.dp,
        loadingStrokeWidth = 2.dp,

        cancelButtonHeight = 42.dp,
        cancelButtonCornerRadius = 18.dp,
        cancelButtonShadowElevation = 0.dp,
        cancelButtonFontSize = 13.sp,
        cancelButtonLineHeight = 16.sp
    )
}

@Composable
fun CardPresentingScreen(
    state: CardPresentingUiState,
    onCancel: () -> Unit
) {
    when (rememberChaiOkDeviceClass()) {
        ChaiOkDeviceClass.SquareCompact -> {
            CardPresentingSquarePremiumScreen(
                state = state,
                onCancel = onCancel
            )
        }

        ChaiOkDeviceClass.Regular -> {
            CardPresentingRegularScreen(
                state = state,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun CardPresentingRegularScreen(
    state: CardPresentingUiState,
    onCancel: () -> Unit
) {
    val metrics = regularCardPresentingMetrics()

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
private fun CardPresentingSquarePremiumScreen(
    state: CardPresentingUiState,
    onCancel: () -> Unit
) {
    val metrics = squarePremiumCardPresentingMetrics()

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
            PremiumLogoHeader(metrics = metrics)

            Spacer(modifier = Modifier.height(metrics.headerToAmountSpacing))

            PremiumAmountHero(
                amountText = state.amountText,
                stage = state.stage,
                metrics = metrics
            )

            Spacer(modifier = Modifier.height(metrics.amountToMainSpacing))

            PremiumPaymentFocus(
                state = state,
                metrics = metrics,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(metrics.mainToButtonSpacing))

            PremiumCancelButton(
                enabled = state.canCancel,
                metrics = metrics,
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun PremiumLogoHeader(
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
private fun PremiumAmountHero(
    amountText: String,
    stage: CardPresentingStage,
    metrics: CardPresentingLayoutMetrics
) {
    val borderColor = when (stage) {
        CardPresentingStage.Approved -> CardPresentingGreenColor.copy(alpha = 0.30f)

        CardPresentingStage.Declined,
        CardPresentingStage.Error,
        CardPresentingStage.Cancelled -> CardPresentingErrorColor.copy(alpha = 0.28f)

        else -> CardPresentingAccentColor.copy(alpha = 0.20f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.amountCardShadowElevation,
                shape = RoundedCornerShape(metrics.amountCardCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.055f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(metrics.amountCardCornerRadius))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(metrics.amountCardCornerRadius)
            )
            .padding(
                horizontal = metrics.amountCardHorizontalPadding,
                vertical = metrics.amountCardVerticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "СУММА К ОПЛАТЕ",
            color = CardPresentingSecondaryTextColor.copy(alpha = 0.82f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.amountLabelFontSize,
            lineHeight = metrics.amountLabelLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
private fun PremiumPaymentFocus(
    state: CardPresentingUiState,
    metrics: CardPresentingLayoutMetrics,
    modifier: Modifier = Modifier
) {
    val accentBrush = paymentAccentBrush(stage = state.stage)

    Column(
        modifier = modifier
            .fillMaxWidth()
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

        if (state.isLoading && !state.stage.shouldShowProcessingSpinner()) {
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
    val accentBrush = paymentAccentBrush(stage = state.stage)

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

private fun paymentAccentBrush(
    stage: CardPresentingStage
): Brush {
    return when (stage) {
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
}

@Composable
private fun AnimatedPaymentVisual(
    stage: CardPresentingStage,
    accentBrush: Brush,
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
        initialValue = 0.16f,
        targetValue = 0.38f,
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
        if (metrics.showPulseCircle && stage.shouldShowNfcPulse()) {
            Box(
                modifier = Modifier
                    .size((metrics.visualPulseBaseSize.value * pulseScale).dp)
                    .clip(CircleShape)
                    .background(
                        CardPresentingAccentColor.copy(alpha = pulseAlpha)
                    )
            )
        }

        NfcWaves(
            modifier = Modifier.size(metrics.visualWavesSize),
            alpha = if (stage.shouldShowNfcPulse()) pulseAlpha else 0f,
            strokeWidth = metrics.nfcWaveStrokeWidth,
            waveRadii = metrics.nfcWaveRadii
        )

        PaymentCenterSurface(
            stage = stage,
            accentBrush = accentBrush,
            iconScale = iconScale,
            metrics = metrics
        )
    }
}

@Composable
private fun PaymentCenterSurface(
    stage: CardPresentingStage,
    accentBrush: Brush,
    iconScale: Float,
    metrics: CardPresentingLayoutMetrics
) {
    val shape = if (metrics.visualIconBoxIsCircle) {
        CircleShape
    } else {
        RoundedCornerShape(metrics.visualIconBoxCornerRadius)
    }

    val showSpinner = metrics.useProcessingSpinner && stage.shouldShowProcessingSpinner()

    val baseModifier = Modifier
        .size(metrics.visualIconBoxSize)
        .shadow(
            elevation = metrics.visualIconBoxShadowElevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.12f),
            spotColor = Color.Black.copy(alpha = 0.20f)
        )
        .clip(shape)

    val surfaceModifier = if (showSpinner) {
        baseModifier
            .background(Color.White)
            .border(
                width = 1.dp,
                color = CardPresentingAccentColor.copy(alpha = 0.26f),
                shape = shape
            )
    } else {
        baseModifier.background(accentBrush)
    }

    Box(
        modifier = surfaceModifier,
        contentAlignment = Alignment.Center
    ) {
        if (showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(metrics.spinnerSize),
                color = CardPresentingAccentColor,
                strokeWidth = metrics.spinnerStrokeWidth
            )
        } else {
            StageIcon(
                stage = stage,
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

@Composable
private fun PremiumCancelButton(
    enabled: Boolean,
    metrics: CardPresentingLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val textColor = if (enabled) {
        CardPresentingErrorColor
    } else {
        CardPresentingSecondaryTextColor.copy(alpha = 0.62f)
    }

    val borderColor = if (enabled) {
        CardPresentingErrorColor.copy(alpha = 0.22f)
    } else {
        CardPresentingStrokeColor
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.cancelButtonHeight)
            .clip(RoundedCornerShape(metrics.cancelButtonCornerRadius))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(metrics.cancelButtonCornerRadius)
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
            color = textColor,
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

private fun CardPresentingStage.shouldShowProcessingSpinner(): Boolean {
    return when (this) {
        CardPresentingStage.Processing,
        CardPresentingStage.Cancelling -> true

        CardPresentingStage.Idle,
        CardPresentingStage.Preparing,
        CardPresentingStage.WaitingForCard,
        CardPresentingStage.CardDetected,
        CardPresentingStage.PinRequired,
        CardPresentingStage.Approved,
        CardPresentingStage.Declined,
        CardPresentingStage.Error,
        CardPresentingStage.Cancelled -> false
    }
}
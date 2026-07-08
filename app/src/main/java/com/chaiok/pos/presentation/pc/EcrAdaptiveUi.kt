package com.chaiok.pos.presentation.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import com.chaiok.pos.presentation.theme.MontserratFontFamily

/** Adaptive visual modes for the unified ECR/PC payment experience. */
enum class EcrLayoutMode { CompactSquare, MediumLandscape, WideLandscape, Large, Portrait }

data class EcrAdaptiveMetrics(
    val mode: EcrLayoutMode,
    val screenPadding: Dp,
    val contentMaxWidth: Dp,
    val cardCornerRadius: Dp,
    val cardPadding: Dp,
    val smallSpacing: Dp,
    val mediumSpacing: Dp,
    val largeSpacing: Dp,
    val titleSize: TextUnit,
    val bodySize: TextUnit,
    val amountSize: TextUnit,
    val buttonHeight: Dp,
    val indicatorSize: Dp,
    val compactScale: Float
)

@Composable
fun rememberEcrAdaptiveMetrics(): EcrAdaptiveMetrics {
    val configuration = LocalConfiguration.current
    val w = configuration.screenWidthDp.dp
    val h = configuration.screenHeightDp.dp
    val mode = when {
        w <= 520.dp && h <= 520.dp -> EcrLayoutMode.CompactSquare
        h > w -> EcrLayoutMode.Portrait
        w >= 1600.dp -> EcrLayoutMode.Large
        w >= 1100.dp -> EcrLayoutMode.WideLandscape
        else -> EcrLayoutMode.MediumLandscape
    }
    return when (mode) {
        EcrLayoutMode.CompactSquare -> EcrAdaptiveMetrics(mode, 16.dp, 480.dp, 24.dp, 14.dp, 6.dp, 10.dp, 16.dp, 20.sp, 12.sp, 40.sp, 56.dp, 144.dp, 1f)
        EcrLayoutMode.MediumLandscape -> EcrAdaptiveMetrics(mode, 28.dp, 760.dp, 28.dp, 20.dp, 8.dp, 14.dp, 22.dp, 24.sp, 15.sp, 46.sp, 58.dp, 160.dp, 1.08f)
        EcrLayoutMode.WideLandscape -> EcrAdaptiveMetrics(mode, 44.dp, 980.dp, 32.dp, 26.dp, 10.dp, 18.dp, 28.dp, 28.sp, 17.sp, 56.sp, 64.dp, 184.dp, 1.25f)
        EcrLayoutMode.Large -> EcrAdaptiveMetrics(mode, 64.dp, 1120.dp, 36.dp, 32.dp, 12.dp, 22.dp, 34.dp, 32.sp, 19.sp, 64.sp, 72.dp, 210.dp, 1.42f)
        EcrLayoutMode.Portrait -> EcrAdaptiveMetrics(mode, 24.dp, 640.dp, 30.dp, 22.dp, 8.dp, 16.dp, 24.dp, 26.sp, 16.sp, 52.sp, 62.dp, 176.dp, 1.12f)
    }
}


data class EcrPaymentMetrics(
    val rootPadding: Dp,
    val headerStart: Dp,
    val tipHeaderTop: Dp,
    val statusHeaderTop: Dp,
    val operationTitleSize: TextUnit,
    val amountSize: TextUnit,
    val tipTitleTopWithFee: Dp,
    val tipTitleTop: Dp,
    val tipRowTopWithFee: Dp,
    val tipRowTop: Dp,
    val tipCardMinWidth: Dp,
    val tipCardMaxWidth: Dp,
    val tipCardHeight: Dp,
    val tipCardHorizontalPadding: Dp,
    val tipCardTextSafetyPadding: Dp,
    val tipCardTitleSize: TextUnit,
    val tipCarouselSidePadding: Dp,
    val tipCarouselSpacing: Dp,
    val noTipsGapWithFee: Dp,
    val noTipsGap: Dp,
    val serviceFeeHorizontalPadding: Dp,
    val serviceFeeVerticalPadding: Dp,
    val cancelTopPadding: Dp,
    val cancelEndPadding: Dp,
    val cancelButtonSize: Dp,
    val cancelIconSize: Dp,
    val statusIndicatorSize: Dp,
    val resultTextOffset: Dp,
    val resultTextSize: TextUnit,
    val customDialogHorizontalPadding: Dp,
    val customDialogCornerRadius: Dp,
    val customDialogPaddingHorizontal: Dp,
    val customDialogPaddingVertical: Dp,
    val customDialogTitleSize: TextUnit,
    val customDialogAmountSize: TextUnit,
    val keypadTouchSize: Dp,
    val keypadDigitSize: TextUnit,
    val keypadIconSize: Dp,
    val dialogButtonHeight: Dp,
    val dialogButtonCornerRadius: Dp,
    val dialogButtonTextSize: TextUnit,
    val cancelPreviousTopPadding: Dp,
    val cancelPreviousTitleSize: TextUnit,
    val cancelPreviousMessageSize: TextUnit
)

fun EcrAdaptiveMetrics.paymentMetrics(): EcrPaymentMetrics {
    val s = when (mode) {
        EcrLayoutMode.CompactSquare -> 1f
        EcrLayoutMode.MediumLandscape -> 1.08f
        EcrLayoutMode.WideLandscape -> 1.22f
        EcrLayoutMode.Large -> 1.42f
        EcrLayoutMode.Portrait -> 1.12f
    }
    fun dp(v: Float) = (v * s).dp
    fun sp(v: Float) = (v * s).sp
    return EcrPaymentMetrics(
        rootPadding = screenPadding,
        headerStart = dp(32f), tipHeaderTop = dp(112f), statusHeaderTop = dp(64f),
        operationTitleSize = sp(16f), amountSize = sp(40f),
        tipTitleTopWithFee = dp(214f), tipTitleTop = dp(262f), tipRowTopWithFee = dp(244f), tipRowTop = dp(302f),
        tipCardMinWidth = dp(140f), tipCardMaxWidth = dp(190f), tipCardHeight = dp(90f),
        tipCardHorizontalPadding = dp(12f), tipCardTextSafetyPadding = dp(8f), tipCardTitleSize = sp(20f),
        tipCarouselSidePadding = dp(16f), tipCarouselSpacing = dp(16f), noTipsGapWithFee = dp(10f), noTipsGap = dp(20f),
        serviceFeeHorizontalPadding = dp(16f), serviceFeeVerticalPadding = dp(16f),
        cancelTopPadding = dp(8f), cancelEndPadding = dp(8f), cancelButtonSize = dp(56f), cancelIconSize = dp(24f),
        statusIndicatorSize = indicatorSize, resultTextOffset = dp(96f), resultTextSize = sp(24f),
        customDialogHorizontalPadding = dp(8f), customDialogCornerRadius = dp(28f), customDialogPaddingHorizontal = dp(14f), customDialogPaddingVertical = dp(12f),
        customDialogTitleSize = sp(19f), customDialogAmountSize = sp(32f), keypadTouchSize = dp(44f), keypadDigitSize = sp(20f), keypadIconSize = dp(22f),
        dialogButtonHeight = dp(42f), dialogButtonCornerRadius = dp(14f), dialogButtonTextSize = sp(14f),
        cancelPreviousTopPadding = dp(300f), cancelPreviousTitleSize = sp(24f), cancelPreviousMessageSize = sp(16f)
    )
}

@Composable
fun EcrScaffold(
    error: Boolean = false,
    metrics: EcrAdaptiveMetrics,
    content: @Composable BoxScope.(EcrAdaptiveMetrics) -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(
                    if (error) Color(0xFF121923) else Color(0xFF151D25),
                    if (error) Color(0xFF2A1B26) else Color(0xFF0E5C91),
                    Color(0xFF1B222A)
                )
            )
        )
    ) { content(metrics) }
}

@Composable
fun EcrGlassCard(modifier: Modifier = Modifier, metrics: EcrAdaptiveMetrics, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(RoundedCornerShape(metrics.cardCornerRadius)).background(Color.White.copy(alpha = 0.10f)).border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(metrics.cardCornerRadius)).padding(metrics.cardPadding), content = content)
}


@Composable fun EcrOperationHeader(title: String, amount: String, metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier) { Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color=Color.White.copy(.72f), fontFamily=MontserratFontFamily, fontWeight=FontWeight.Medium, fontSize=metrics.titleSize, textAlign=TextAlign.Center); if(amount.isNotBlank()) Text(amount, color=Color.White, fontFamily=MontserratFontFamily, fontWeight=FontWeight.Bold, fontSize=metrics.amountSize, textAlign=TextAlign.Center) } }
@Composable fun EcrAmountBlock(amount: String, metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier) = EcrOperationHeader("к оплате", amount, metrics, modifier)
@Composable fun EcrStatusBlock(text: String, metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier) { Text(text, color=Color.White.copy(.86f), fontFamily=MontserratFontFamily, fontSize=metrics.bodySize, textAlign=TextAlign.Center, modifier=modifier) }
@Composable fun EcrPrimaryButton(text: String, enabled: Boolean, metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier, onClick: () -> Unit) { Box(modifier.height(metrics.buttonHeight).background(if(enabled) Color(0xFF20D6D2) else Color(0x5520D6D2), RoundedCornerShape(metrics.cardCornerRadius)).clickable(enabled=enabled,onClick=onClick), contentAlignment=Alignment.Center){ Text(text, color=Color.White, fontFamily=MontserratFontFamily, fontWeight=FontWeight.SemiBold, fontSize=metrics.bodySize) } }
@Composable fun EcrSecondaryButton(text: String, enabled: Boolean, metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier, onClick: () -> Unit) { Box(modifier.height(metrics.buttonHeight).background(Color.White.copy(if(enabled) .10f else .05f), RoundedCornerShape(metrics.cardCornerRadius)).border(1.dp, Color.White.copy(.20f), RoundedCornerShape(metrics.cardCornerRadius)).clickable(enabled=enabled,onClick=onClick), contentAlignment=Alignment.Center){ Text(text, color=Color.White.copy(if(enabled) .95f else .45f), fontFamily=MontserratFontFamily, fontWeight=FontWeight.SemiBold, fontSize=metrics.bodySize) } }
@Composable fun EcrProgressBlock(metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier) { CircularProgressIndicator(modifier=modifier, color=Color(0xFF20D6D2), strokeWidth=4.dp) }
@Composable fun EcrResultBlock(text: String, metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier) = EcrStatusBlock(text, metrics, modifier)
@Composable fun EcrCardPresentingBlock(text: String, metrics: EcrAdaptiveMetrics, modifier: Modifier = Modifier) = EcrStatusBlock(text, metrics, modifier)

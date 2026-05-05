package com.chaiok.pos.presentation.tipselection

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.components.WaiterProfileCardHeader
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val TipSelectionScreenColor = Color.White
private val TipSelectionPrimaryTextColor = Color(0xFF1B2128)
private val TipSelectionSecondaryTextColor = Color(0xFF69707A)
private val TipSelectionCardColor = Color(0xFFF7F8FA)
private val TipSelectionSoftCardColor = Color(0xFFF0F3F7)
private val TipSelectionStrokeColor = Color(0xFFE2E7EF)
private val TipSelectionAccentColor = Color(0xFF087BE8)
private val TipSelectionGreenColor = Color(0xFF14B8A6)
private val TipSelectionWarningColor = Color(0xFFFFB547)

private data class TipSelectionLayoutMetrics(
    val isSquareCompact: Boolean,
    val headerSpacer: Dp,
    val horizontalPadding: Dp,
    val bottomPadding: Dp,
    val contentCardSpacing: Dp,
    val payButtonTopSpacer: Dp,

    val billFontSize: Int,
    val billLineHeight: Int,

    val tipCardCorner: Dp,
    val tipCardPaddingHorizontal: Dp,
    val tipCardPaddingVertical: Dp,
    val tipCardShadow: Dp,
    val tipCardsSpacing: Dp,

    val percentCardWidth: Dp,
    val percentCardHeight: Dp,
    val percentCardCorner: Dp,
    val percentCardPaddingHorizontal: Dp,
    val percentCardPaddingVertical: Dp,
    val percentFontSize: Int,
    val percentLineHeight: Int,
    val percentAmountFontSize: Int,
    val percentAmountLineHeight: Int,

    val customTipHeight: Dp,
    val customTipIconSize: Dp,
    val customTipIconCorner: Dp,
    val customTipTitleSize: Int,
    val customTipAmountSize: Int,

    val reviewCardPaddingHorizontal: Dp,
    val reviewCardPaddingVertical: Dp,
    val ratingRowHeight: Dp,
    val ratingTitleSize: Int,
    val ratingTitleLineHeight: Int,
    val starButtonSize: Dp,
    val starIconSize: Dp,
    val starSpacing: Dp,

    val serviceFeeHeight: Dp,
    val serviceFeeCorner: Dp,
    val serviceFeeTitleSize: Int,
    val serviceFeeSubtitleSize: Int,
    val serviceFeeHorizontalPadding: Dp,

    val payButtonHeight: Dp,
    val payButtonCorner: Dp,
    val payButtonFontSize: Int,

    val resultTopPadding: Dp,
    val resultCardPadding: Dp,
    val resultIconSize: Dp,
    val resultTitleSize: Int,
    val resultTitleLineHeight: Int,
    val resultAmountSize: Int,
    val resultMessageSize: Int,
    val resultButtonHeight: Dp,
    val resultButtonFontSize: Int
)

@Composable
fun TipSelectionScreen(
    state: TipSelectionUiState,
    onBack: () -> Unit,
    onPreset: (Int) -> Unit,
    onCustomStart: () -> Unit,
    onCustomSet: (String) -> Unit,
    onDismissCustom: () -> Unit,
    onPay: () -> Unit,
    onSnackbarShown: () -> Unit,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onServiceFeeToggle: (Boolean) -> Unit = {},
    onKitchenEvaluation: (Int) -> Unit = {},
    onServiceEvaluation: (Int) -> Unit = {}
) {
    val snackState = remember { SnackbarHostState() }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp

    val metrics = if (isSquareCompact) {
        TipSelectionLayoutMetrics(
            isSquareCompact = true,
            headerSpacer = 6.dp,
            horizontalPadding = 14.dp,
            bottomPadding = 8.dp,
            contentCardSpacing = 6.dp,
            payButtonTopSpacer = 6.dp,

            billFontSize = 12,
            billLineHeight = 15,

            tipCardCorner = 18.dp,
            tipCardPaddingHorizontal = 8.dp,
            tipCardPaddingVertical = 6.dp,
            tipCardShadow = 4.dp,
            tipCardsSpacing = 6.dp,

            percentCardWidth = 82.dp,
            percentCardHeight = 44.dp,
            percentCardCorner = 15.dp,
            percentCardPaddingHorizontal = 8.dp,
            percentCardPaddingVertical = 5.dp,
            percentFontSize = 14,
            percentLineHeight = 16,
            percentAmountFontSize = 9,
            percentAmountLineHeight = 11,

            customTipHeight = 38.dp,
            customTipIconSize = 24.dp,
            customTipIconCorner = 9.dp,
            customTipTitleSize = 11,
            customTipAmountSize = 10,

            reviewCardPaddingHorizontal = 8.dp,
            reviewCardPaddingVertical = 6.dp,
            ratingRowHeight = 24.dp,
            ratingTitleSize = 11,
            ratingTitleLineHeight = 14,
            starButtonSize = 23.dp,
            starIconSize = 13.dp,
            starSpacing = 3.dp,

            serviceFeeHeight = 46.dp,
            serviceFeeCorner = 17.dp,
            serviceFeeTitleSize = 11,
            serviceFeeSubtitleSize = 9,
            serviceFeeHorizontalPadding = 10.dp,

            payButtonHeight = 44.dp,
            payButtonCorner = 17.dp,
            payButtonFontSize = 13,

            resultTopPadding = 4.dp,
            resultCardPadding = 14.dp,
            resultIconSize = 54.dp,
            resultTitleSize = 17,
            resultTitleLineHeight = 21,
            resultAmountSize = 15,
            resultMessageSize = 11,
            resultButtonHeight = 40.dp,
            resultButtonFontSize = 12
        )
    } else {
        TipSelectionLayoutMetrics(
            isSquareCompact = false,
            headerSpacer = 18.dp,
            horizontalPadding = 24.dp,
            bottomPadding = 20.dp,
            contentCardSpacing = 16.dp,
            payButtonTopSpacer = 10.dp,

            billFontSize = 17,
            billLineHeight = 21,

            tipCardCorner = 24.dp,
            tipCardPaddingHorizontal = 12.dp,
            tipCardPaddingVertical = 12.dp,
            tipCardShadow = 6.dp,
            tipCardsSpacing = 8.dp,

            percentCardWidth = 106.dp,
            percentCardHeight = 74.dp,
            percentCardCorner = 20.dp,
            percentCardPaddingHorizontal = 12.dp,
            percentCardPaddingVertical = 9.dp,
            percentFontSize = 18,
            percentLineHeight = 21,
            percentAmountFontSize = 12,
            percentAmountLineHeight = 15,

            customTipHeight = 52.dp,
            customTipIconSize = 32.dp,
            customTipIconCorner = 11.dp,
            customTipTitleSize = 14,
            customTipAmountSize = 12,

            reviewCardPaddingHorizontal = 12.dp,
            reviewCardPaddingVertical = 12.dp,
            ratingRowHeight = 34.dp,
            ratingTitleSize = 14,
            ratingTitleLineHeight = 18,
            starButtonSize = 32.dp,
            starIconSize = 17.dp,
            starSpacing = 5.dp,

            serviceFeeHeight = 62.dp,
            serviceFeeCorner = 22.dp,
            serviceFeeTitleSize = 14,
            serviceFeeSubtitleSize = 12,
            serviceFeeHorizontalPadding = 14.dp,

            payButtonHeight = 56.dp,
            payButtonCorner = 22.dp,
            payButtonFontSize = 16,

            resultTopPadding = 8.dp,
            resultCardPadding = 22.dp,
            resultIconSize = 88.dp,
            resultTitleSize = 24,
            resultTitleLineHeight = 28,
            resultAmountSize = 20,
            resultMessageSize = 14,
            resultButtonHeight = 52.dp,
            resultButtonFontSize = 15
        )
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackState.showSnackbar(message)
            onSnackbarShown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TipSelectionScreenColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                WaiterProfileCardHeader(
                    waiterName = state.waiterName,
                    waiterStatus = state.waiterStatus,
                    background = state.tileBackground
                )

                TiplyBackTopAppBar(
                    title = "Чаевые",
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter),
                    elevation = if (metrics.isSquareCompact) 8.dp else 14.dp,
                    ambientAlpha = if (metrics.isSquareCompact) 0.32f else 0.64f,
                    spotAlpha = if (metrics.isSquareCompact) 0.42f else 0.72f
                )
            }

            Spacer(modifier = Modifier.height(metrics.headerSpacer))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.horizontalPadding)
                    .padding(bottom = metrics.bottomPadding)
            ) {
                val isResultState =
                    state.paymentState is TipPaymentUiState.Approved ||
                            state.paymentState is TipPaymentUiState.Declined

                if (isResultState) {
                    PaymentResultContent(
                        state = state,
                        metrics = metrics,
                        onDone = onDone,
                        onRetry = onRetry,
                        onBack = onBack
                    )
                } else {
                    TipSelectionContent(
                        state = state,
                        metrics = metrics,
                        onPreset = onPreset,
                        onCustomStart = onCustomStart,
                        onServiceFeeToggle = onServiceFeeToggle,
                        onKitchenEvaluation = onKitchenEvaluation,
                        onServiceEvaluation = onServiceEvaluation,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(metrics.payButtonTopSpacer))

                    GradientPayButton(
                        amount = state.totalAmount,
                        enabled = state.isPayEnabled,
                        loading = state.paymentState == TipPaymentUiState.Processing,
                        metrics = metrics,
                        onClick = onPay
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )
    }

    if (state.showCustomTipDialog) {
        CustomTipDialog(
            initialValue = state.customTipAmount?.toInt()?.toString().orEmpty(),
            onDismiss = onDismissCustom,
            onSave = onCustomSet
        )
    }
}

@Composable
private fun TipSelectionContent(
    state: TipSelectionUiState,
    metrics: TipSelectionLayoutMetrics,
    onPreset: (Int) -> Unit,
    onCustomStart: () -> Unit,
    onServiceFeeToggle: (Boolean) -> Unit,
    onKitchenEvaluation: (Int) -> Unit,
    onServiceEvaluation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        BillAndTipsInfoRow(
            billAmount = state.billAmount,
            tipAmount = state.selectedTipAmount,
            metrics = metrics
        )

        Spacer(modifier = Modifier.height(if (metrics.isSquareCompact) 4.dp else 8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(metrics.contentCardSpacing)
        ) {
            TipSelectionCard(
                state = state,
                metrics = metrics,
                onPreset = onPreset,
                onCustomStart = onCustomStart
            )

            ReviewEvaluationCard(
                kitchenEvaluation = state.kitchenEvaluation,
                serviceEvaluation = state.serviceEvaluation,
                metrics = metrics,
                onKitchenEvaluation = onKitchenEvaluation,
                onServiceEvaluation = onServiceEvaluation
            )

            if (state.serviceFeePercent > 0.0) {
                ServiceFeeSwitchCard(
                    checked = state.isServiceFeeEnabled,
                    percent = state.serviceFeePercent,
                    amount = state.serviceFeeAmount,
                    metrics = metrics,
                    onCheckedChange = onServiceFeeToggle
                )
            }
        }
    }
}

@Composable
private fun TipSelectionCard(
    state: TipSelectionUiState,
    metrics: TipSelectionLayoutMetrics,
    onPreset: (Int) -> Unit,
    onCustomStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.tipCardShadow,
                shape = RoundedCornerShape(metrics.tipCardCorner),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(metrics.tipCardCorner))
            .background(TipSelectionCardColor)
            .padding(
                horizontal = metrics.tipCardPaddingHorizontal,
                vertical = metrics.tipCardPaddingVertical
            )
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(metrics.tipCardsSpacing)
        ) {
            itemsIndexed(state.availablePercents) { index, percent ->
                val selected = !state.isCustomSelected && state.selectedPercentIndex == index
                val tipAmount = state.calculateTipByPercent(percent)

                TipPercentCard(
                    percentText = formatPercent(percent),
                    amountText = formatRubles(tipAmount),
                    selected = selected,
                    metrics = metrics,
                    onClick = { onPreset(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(if (metrics.isSquareCompact) 4.dp else 8.dp))

        CustomTipCard(
            selected = state.isCustomSelected,
            amountText = if (state.customTipAmount != null) {
                formatRubles(state.customTipAmount)
            } else {
                "Указать сумму"
            },
            metrics = metrics,
            onClick = onCustomStart
        )
    }
}

@Composable
private fun ReviewEvaluationCard(
    kitchenEvaluation: Int,
    serviceEvaluation: Int,
    metrics: TipSelectionLayoutMetrics,
    onKitchenEvaluation: (Int) -> Unit,
    onServiceEvaluation: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (metrics.isSquareCompact) 4.dp else 6.dp,
                shape = RoundedCornerShape(metrics.tipCardCorner),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(metrics.tipCardCorner))
            .background(TipSelectionCardColor)
            .padding(
                horizontal = metrics.reviewCardPaddingHorizontal,
                vertical = metrics.reviewCardPaddingVertical
            ),
        verticalArrangement = Arrangement.spacedBy(if (metrics.isSquareCompact) 4.dp else 10.dp)
    ) {
        CompactRatingRow(
            title = "Как вам кухня",
            value = kitchenEvaluation,
            metrics = metrics,
            onSelect = onKitchenEvaluation
        )

        CompactRatingRow(
            title = "Как вам сервис",
            value = serviceEvaluation,
            metrics = metrics,
            onSelect = onServiceEvaluation
        )
    }
}

@Composable
private fun CompactRatingRow(
    title: String,
    value: Int,
    metrics: TipSelectionLayoutMetrics,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.ratingRowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = TipSelectionPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.ratingTitleSize.sp,
            lineHeight = metrics.ratingTitleLineHeight.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(metrics.starSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val starValue = index + 1

                CompactStarButton(
                    selected = starValue <= value,
                    metrics = metrics,
                    onClick = { onSelect(starValue) }
                )
            }
        }
    }
}

@Composable
private fun CompactStarButton(
    selected: Boolean,
    metrics: TipSelectionLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = tween(durationMillis = 160),
        label = "compactStarScale"
    )

    Box(
        modifier = Modifier
            .size(metrics.starButtonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (selected) {
                    if (metrics.isSquareCompact) 3.dp else 5.dp
                } else {
                    0.dp
                },
                shape = CircleShape,
                clip = false,
                ambientColor = TipSelectionWarningColor.copy(alpha = 0.16f),
                spotColor = TipSelectionWarningColor.copy(alpha = 0.22f)
            )
            .clip(CircleShape)
            .background(
                brush = if (selected) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFFC94D),
                            Color(0xFFFFA63D)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White,
                            Color(0xFFF3F6FA)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else TipSelectionStrokeColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = if (selected) Color.White else Color(0xFFC5CED9),
            modifier = Modifier.size(metrics.starIconSize)
        )
    }
}

@Composable
private fun BillAndTipsInfoRow(
    billAmount: Double,
    tipAmount: Double,
    metrics: TipSelectionLayoutMetrics
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InlineAmountText(
            label = "Чек:",
            amount = formatRubles(billAmount),
            metrics = metrics,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        InlineAmountText(
            label = "Чаевые:",
            amount = formatRubles(tipAmount),
            metrics = metrics,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun InlineAmountText(
    label: String,
    amount: String,
    metrics: TipSelectionLayoutMetrics,
    modifier: Modifier = Modifier,
    textAlign: TextAlign
) {
    Text(
        text = "$label $amount",
        modifier = modifier,
        color = TipSelectionPrimaryTextColor,
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.billFontSize.sp,
        lineHeight = metrics.billLineHeight.sp,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ServiceFeeSwitchCard(
    checked: Boolean,
    percent: Double,
    amount: Double,
    metrics: TipSelectionLayoutMetrics,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.serviceFeeHeight)
            .shadow(
                elevation = if (checked) {
                    if (metrics.isSquareCompact) 4.dp else 6.dp
                } else {
                    if (metrics.isSquareCompact) 2.dp else 3.dp
                },
                shape = RoundedCornerShape(metrics.serviceFeeCorner),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(metrics.serviceFeeCorner))
            .background(
                brush = if (checked) {
                    Brush.linearGradient(
                        listOf(
                            TipSelectionAccentColor.copy(alpha = 0.10f),
                            TipSelectionGreenColor.copy(alpha = 0.12f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White,
                            TipSelectionSoftCardColor
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (checked) {
                    TipSelectionAccentColor.copy(alpha = 0.24f)
                } else {
                    TipSelectionStrokeColor
                },
                shape = RoundedCornerShape(metrics.serviceFeeCorner)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = metrics.serviceFeeHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Компенсация комиссии",
                color = TipSelectionPrimaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.serviceFeeTitleSize.sp,
                lineHeight = (metrics.serviceFeeTitleSize + 3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(if (metrics.isSquareCompact) 0.dp else 2.dp))

            Text(
                text = "${formatPercent(percent)} от чаевых · ${formatRubles(amount)}",
                color = TipSelectionSecondaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.serviceFeeSubtitleSize.sp,
                lineHeight = (metrics.serviceFeeSubtitleSize + 3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(if (metrics.isSquareCompact) 6.dp else 10.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TipSelectionAccentColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFC9D2DE),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun TipPercentCard(
    percentText: String,
    amountText: String,
    selected: Boolean,
    metrics: TipSelectionLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(metrics.percentCardCorner)

    Column(
        modifier = Modifier
            .width(metrics.percentCardWidth)
            .height(metrics.percentCardHeight)
            .shadow(
                elevation = if (selected) {
                    if (metrics.isSquareCompact) 4.dp else 8.dp
                } else {
                    0.dp
                },
                shape = shape,
                clip = false,
                ambientColor = TipSelectionAccentColor.copy(alpha = 0.14f),
                spotColor = TipSelectionAccentColor.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(
                brush = if (selected) {
                    Brush.linearGradient(
                        listOf(
                            TipSelectionAccentColor,
                            TipSelectionGreenColor
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White,
                            Color(0xFFF0F3F7)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else TipSelectionStrokeColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = metrics.percentCardPaddingHorizontal,
                vertical = metrics.percentCardPaddingVertical
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = percentText,
            color = if (selected) Color.White else TipSelectionPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.percentFontSize.sp,
            lineHeight = metrics.percentLineHeight.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(if (metrics.isSquareCompact) 1.dp else 3.dp))

        Text(
            text = amountText,
            color = if (selected) {
                Color.White.copy(alpha = 0.92f)
            } else {
                TipSelectionSecondaryTextColor
            },
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.percentAmountFontSize.sp,
            lineHeight = metrics.percentAmountLineHeight.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CustomTipCard(
    selected: Boolean,
    amountText: String,
    metrics: TipSelectionLayoutMetrics,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(metrics.percentCardCorner)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.customTipHeight)
            .shadow(
                elevation = if (selected) {
                    if (metrics.isSquareCompact) 4.dp else 6.dp
                } else {
                    0.dp
                },
                shape = shape,
                clip = false,
                ambientColor = TipSelectionAccentColor.copy(alpha = 0.08f),
                spotColor = TipSelectionAccentColor.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(
                brush = if (selected) {
                    Brush.linearGradient(
                        listOf(
                            TipSelectionAccentColor.copy(alpha = 0.12f),
                            TipSelectionGreenColor.copy(alpha = 0.14f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White,
                            TipSelectionSoftCardColor
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    TipSelectionAccentColor.copy(alpha = 0.28f)
                } else {
                    TipSelectionStrokeColor
                },
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (metrics.isSquareCompact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(metrics.customTipIconSize)
                .clip(RoundedCornerShape(metrics.customTipIconCorner))
                .background(
                    brush = if (selected) {
                        Brush.linearGradient(
                            listOf(
                                TipSelectionAccentColor,
                                TipSelectionGreenColor
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFEFF3F8)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "₽",
                color = if (selected) Color.White else TipSelectionAccentColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (metrics.isSquareCompact) 13.sp else 17.sp,
                lineHeight = if (metrics.isSquareCompact) 15.sp else 19.sp
            )
        }

        Spacer(modifier = Modifier.width(if (metrics.isSquareCompact) 8.dp else 10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Своя сумма",
                color = TipSelectionPrimaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.customTipTitleSize.sp,
                lineHeight = (metrics.customTipTitleSize + 3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = amountText,
                color = TipSelectionSecondaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.customTipAmountSize.sp,
                lineHeight = (metrics.customTipAmountSize + 3).sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GradientPayButton(
    amount: Double,
    enabled: Boolean,
    loading: Boolean,
    metrics: TipSelectionLayoutMetrics,
    onClick: () -> Unit
) {
    val animatedAmountKopecks by animateIntAsState(
        targetValue = amountToKopecks(amount),
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "PayAmountCounterAnimation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.payButtonHeight)
            .shadow(
                elevation = if (enabled) {
                    if (metrics.isSquareCompact) 5.dp else 8.dp
                } else {
                    0.dp
                },
                shape = RoundedCornerShape(metrics.payButtonCorner),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (enabled) 0.12f else 0f),
                spotColor = Color.Black.copy(alpha = if (enabled) 0.18f else 0f)
            )
            .clip(RoundedCornerShape(metrics.payButtonCorner))
            .background(
                brush = if (enabled) {
                    Brush.linearGradient(
                        listOf(
                            TipSelectionAccentColor,
                            TipSelectionGreenColor
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFD6DEE9),
                            Color(0xFFD6DEE9)
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
            text = if (loading) {
                "Оплата..."
            } else {
                "Оплатить ${formatKopecks(animatedAmountKopecks)}"
            },
            color = if (enabled) Color.White else Color(0xFF8B96A5),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.payButtonFontSize.sp,
            lineHeight = (metrics.payButtonFontSize + 4).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PaymentResultContent(
    state: TipSelectionUiState,
    metrics: TipSelectionLayoutMetrics,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val approved = state.paymentState is TipPaymentUiState.Approved
    val shown =
        state.paymentState is TipPaymentUiState.Approved ||
                state.paymentState is TipPaymentUiState.Declined

    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.85f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "resultScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "resultAlpha"
    )

    val accent = if (approved) {
        Brush.linearGradient(listOf(TipSelectionGreenColor, TipSelectionAccentColor))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E8E)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = metrics.resultTopPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .shadow(
                    elevation = if (metrics.isSquareCompact) 5.dp else 8.dp,
                    shape = RoundedCornerShape(if (metrics.isSquareCompact) 22.dp else 28.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .clip(RoundedCornerShape(if (metrics.isSquareCompact) 22.dp else 28.dp))
                .background(TipSelectionCardColor)
                .padding(metrics.resultCardPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(metrics.resultIconSize)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (approved) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (metrics.isSquareCompact) 26.dp else 32.dp)
                    )
                }

                Spacer(Modifier.height(if (metrics.isSquareCompact) 8.dp else 14.dp))

                Text(
                    text = if (approved) "Оплата одобрена" else "Оплата отклонена",
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TipSelectionPrimaryTextColor,
                    fontSize = metrics.resultTitleSize.sp,
                    lineHeight = metrics.resultTitleLineHeight.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(if (metrics.isSquareCompact) 4.dp else 8.dp))

                Text(
                    text = formatRubles(state.totalAmount),
                    fontFamily = MontserratFontFamily,
                    color = TipSelectionPrimaryTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = metrics.resultAmountSize.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val msg = when (val paymentState = state.paymentState) {
                    is TipPaymentUiState.Approved -> paymentState.message
                    is TipPaymentUiState.Declined -> paymentState.message
                    else -> null
                }

                if (!msg.isNullOrBlank()) {
                    Spacer(Modifier.height(if (metrics.isSquareCompact) 4.dp else 8.dp))

                    Text(
                        text = msg,
                        color = TipSelectionSecondaryTextColor,
                        textAlign = TextAlign.Center,
                        fontFamily = MontserratFontFamily,
                        fontSize = metrics.resultMessageSize.sp,
                        lineHeight = (metrics.resultMessageSize + 4).sp,
                        maxLines = if (metrics.isSquareCompact) 2 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(if (metrics.isSquareCompact) 10.dp else 18.dp))

                if (approved) {
                    ResultActionButton(
                        text = "Готово",
                        metrics = metrics,
                        onClick = onDone,
                        gradient = Brush.linearGradient(
                            listOf(
                                TipSelectionAccentColor,
                                TipSelectionGreenColor
                            )
                        )
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            if (metrics.isSquareCompact) 6.dp else 10.dp
                        )
                    ) {
                        ResultActionButton(
                            text = if (metrics.isSquareCompact) "Снова" else "Попробовать снова",
                            metrics = metrics,
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            gradient = Brush.linearGradient(
                                listOf(
                                    TipSelectionAccentColor,
                                    TipSelectionGreenColor
                                )
                            )
                        )

                        ResultActionButton(
                            text = "Назад",
                            metrics = metrics,
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            gradient = Brush.linearGradient(
                                listOf(
                                    Color(0xFFE1E7EF),
                                    Color(0xFFE1E7EF)
                                )
                            ),
                            textColor = TipSelectionSecondaryTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultActionButton(
    text: String,
    metrics: TipSelectionLayoutMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush,
    textColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.resultButtonHeight)
            .shadow(
                elevation = if (metrics.isSquareCompact) 4.dp else 6.dp,
                shape = RoundedCornerShape(if (metrics.isSquareCompact) 16.dp else 20.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(if (metrics.isSquareCompact) 16.dp else 20.dp))
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.resultButtonFontSize.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CustomTipDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp

    var value by remember(initialValue) {
        mutableStateOf(initialValue.filter(Char::isDigit))
    }

    val normalizedValue = value.trimStart('0')
    val numericValue = normalizedValue.toIntOrNull() ?: 0
    val confirmEnabled = numericValue > 0

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSquareCompact) 0.dp else 2.dp)
                .clip(RoundedCornerShape(if (isSquareCompact) 28.dp else 38.dp))
                .background(Color.White)
                .padding(
                    start = if (isSquareCompact) 14.dp else 20.dp,
                    end = if (isSquareCompact) 14.dp else 20.dp,
                    top = if (isSquareCompact) 14.dp else 22.dp,
                    bottom = if (isSquareCompact) 12.dp else 18.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Своя сумма",
                        color = TipSelectionPrimaryTextColor,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSquareCompact) 16.sp else 20.sp,
                        lineHeight = if (isSquareCompact) 20.sp else 24.sp
                    )

                    Spacer(modifier = Modifier.height(if (isSquareCompact) 2.dp else 6.dp))

                    Text(
                        text = "Введите сумму чаевых",
                        color = TipSelectionSecondaryTextColor,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = if (isSquareCompact) 11.sp else 14.sp,
                        lineHeight = if (isSquareCompact) 15.sp else 19.sp
                    )
                }

                DialogActionText(
                    text = "Отмена",
                    color = Color(0xFFFF4545),
                    compact = isSquareCompact,
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(if (isSquareCompact) 10.dp else 22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSquareCompact) 54.dp else 82.dp)
                    .clip(RoundedCornerShape(if (isSquareCompact) 20.dp else 26.dp))
                    .background(Color(0xFFF7F8FA))
                    .border(
                        width = 1.dp,
                        color = TipSelectionStrokeColor,
                        shape = RoundedCornerShape(if (isSquareCompact) 20.dp else 26.dp)
                    )
                    .padding(horizontal = if (isSquareCompact) 12.dp else 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatAmountInput(value),
                    color = TipSelectionPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = when {
                        isSquareCompact && value.length <= 4 -> 28.sp
                        isSquareCompact && value.length <= 6 -> 24.sp
                        isSquareCompact -> 21.sp
                        value.length <= 4 -> 40.sp
                        value.length <= 6 -> 34.sp
                        else -> 28.sp
                    },
                    lineHeight = if (isSquareCompact) 32.sp else 44.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(if (isSquareCompact) 8.dp else 18.dp))

            TiplyNumericKeypad(
                digitColor = TipSelectionPrimaryTextColor,
                touchSize = if (isSquareCompact) 48.dp else null,
                digitFontSize = if (isSquareCompact) 22.sp else null,
                iconSize = if (isSquareCompact) 24.dp else null,
                onDigit = { digit ->
                    if (value.length < 7) {
                        val nextValue = value + digit
                        value = nextValue.trimStart('0').ifBlank { "0" }
                    }
                },
                onDelete = {
                    value = value.dropLast(1)
                },
                onConfirm = {
                    if (confirmEnabled) {
                        onSave(numericValue.toString())
                    }
                },
                confirmEnabled = confirmEnabled,
                isLoading = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DialogActionText(
    text: String,
    color: Color,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Text(
        text = text,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = if (compact) 3.dp else 6.dp),
        color = color,
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = if (compact) 14.sp else 18.sp,
        lineHeight = if (compact) 18.sp else 22.sp,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

private fun formatRubles(amount: Double): String =
    formatKopecks(amountToKopecks(amount))

private fun amountToKopecks(amount: Double): Int =
    kotlin.math.round(amount.coerceAtLeast(0.0) * 100.0).toInt()

private fun formatKopecks(kopecks: Int): String {
    val rubles = kopecks / 100
    val cents = kotlin.math.abs(kopecks % 100)
    val groupedRubles = rubles
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return if (cents == 0) {
        "$groupedRubles ₽"
    } else {
        "$groupedRubles,${cents.toString().padStart(2, '0')} ₽"
    }
}

private fun formatAmountInput(input: String): String {
    val amount = input
        .filter(Char::isDigit)
        .trimStart('0')
        .toIntOrNull()

    return if (amount == null || amount <= 0) {
        "₽"
    } else {
        formatRubles(amount.toDouble())
    }
}

private fun formatPercent(percent: Double): String {
    val intValue = percent.toInt()

    return if (percent == intValue.toDouble()) {
        "$intValue%"
    } else {
        "${percent.toString().replace('.', ',')}%"
    }
}
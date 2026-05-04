package com.chaiok.pos.presentation.tipselection

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaiok.pos.R
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
    onServiceFeeToggle: (Boolean) -> Unit = {}
) {
    val snackState = remember { SnackbarHostState() }

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
                    waiterStatus = state.waiterStatus
                )

                TipSelectionTopAppBar(
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                val isResultState =
                    state.paymentState is TipPaymentUiState.Approved ||
                            state.paymentState is TipPaymentUiState.Declined

                if (isResultState) {
                    PaymentResultContent(
                        state = state,
                        onDone = onDone,
                        onRetry = onRetry,
                        onBack = onBack
                    )
                } else {
                    TipSelectionContent(
                        state = state,
                        onPreset = onPreset,
                        onCustomStart = onCustomStart,
                        onServiceFeeToggle = onServiceFeeToggle
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    GradientPayButton(
                        amount = state.totalAmount,
                        enabled = state.isPayEnabled,
                        loading = state.paymentState == TipPaymentUiState.Processing,
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
    onPreset: (Int) -> Unit,
    onCustomStart: () -> Unit,
    onServiceFeeToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        BillAndTipsInfoRow(
            billAmount = state.billAmount,
            tipAmount = state.selectedTipAmount
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(TipSelectionCardColor)
                .padding(16.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(state.availablePercents) { index, percent ->
                    val selected = !state.isCustomSelected && state.selectedPercentIndex == index
                    val tipAmount = state.calculateTipByPercent(percent)

                    TipPercentCard(
                        percentText = "${percent.toInt()}%",
                        amountText = formatRubles(tipAmount),
                        selected = selected,
                        onClick = { onPreset(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CustomTipCard(
                selected = state.isCustomSelected,
                amountText = if (state.customTipAmount != null) {
                    formatRubles(state.customTipAmount)
                } else {
                    "Указать сумму"
                },
                onClick = onCustomStart
            )
        }

        if (state.serviceFeePercent > 0.0) {
            Spacer(modifier = Modifier.height(14.dp))

            ServiceFeeSwitchCard(
                checked = state.isServiceFeeEnabled,
                percent = state.serviceFeePercent,
                amount = state.serviceFeeAmount,
                onCheckedChange = onServiceFeeToggle
            )
        }
    }
}

@Composable
private fun BillAndTipsInfoRow(
    billAmount: Double,
    tipAmount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InlineAmountText(
            label = "Чек:",
            amount = formatRubles(billAmount),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        InlineAmountText(
            label = "Чаевые:",
            amount = formatRubles(tipAmount),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun InlineAmountText(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign
) {
    Text(
        text = "$label $amount",
        modifier = modifier,
        color = TipSelectionPrimaryTextColor,
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
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
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .shadow(
                elevation = if (checked) 8.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(24.dp))
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
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp),
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
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatPercent(percent)} от чаевых · ${formatRubles(amount)}",
                color = TipSelectionSecondaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

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
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .width(122.dp)
            .height(104.dp)
            .shadow(
                elevation = if (selected) 10.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = TipSelectionAccentColor.copy(alpha = 0.16f),
                spotColor = TipSelectionAccentColor.copy(alpha = 0.24f)
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
            .padding(14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = percentText,
            color = if (selected) Color.White else TipSelectionPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = amountText,
            color = if (selected) Color.White.copy(alpha = 0.92f) else TipSelectionSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 18.sp,
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
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(
                elevation = if (selected) 8.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = TipSelectionAccentColor.copy(alpha = 0.10f),
                spotColor = TipSelectionAccentColor.copy(alpha = 0.16f)
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
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
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
                fontSize = 19.sp,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Своя сумма",
                color = TipSelectionPrimaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = amountText,
                color = TipSelectionSecondaryTextColor,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
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
            .height(58.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (enabled) 0.12f else 0f),
                spotColor = Color.Black.copy(alpha = if (enabled) 0.18f else 0f)
            )
            .clip(RoundedCornerShape(24.dp))
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
            fontSize = 16.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PaymentResultContent(
    state: TipSelectionUiState,
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
            .padding(top = 8.dp)
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
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(TipSelectionCardColor)
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(44.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (approved) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (approved) "Оплата одобрена" else "Оплата отклонена",
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TipSelectionPrimaryTextColor,
                    fontSize = 24.sp,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = formatRubles(state.totalAmount),
                    fontFamily = MontserratFontFamily,
                    color = TipSelectionPrimaryTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )

                val msg = when (val paymentState = state.paymentState) {
                    is TipPaymentUiState.Approved -> paymentState.message
                    is TipPaymentUiState.Declined -> paymentState.message
                    else -> null
                }

                if (!msg.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = msg,
                        color = TipSelectionSecondaryTextColor,
                        textAlign = TextAlign.Center,
                        fontFamily = MontserratFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                }

                Spacer(Modifier.height(18.dp))

                if (approved) {
                    ResultActionButton(
                        text = "Готово",
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultActionButton(
                            text = "Попробовать снова",
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush,
    textColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(20.dp))
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
            fontSize = 15.sp,
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
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(Color.White)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 22.dp,
                    bottom = 18.dp
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
                        fontSize = 20.sp,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Введите сумму чаевых",
                        color = TipSelectionSecondaryTextColor,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                }

                DialogActionText(
                    text = "Отмена",
                    color = Color(0xFFFF4545),
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFF7F8FA))
                    .border(
                        width = 1.dp,
                        color = TipSelectionStrokeColor,
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatAmountInput(value),
                    color = TipSelectionPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = when {
                        value.length <= 4 -> 40.sp
                        value.length <= 6 -> 34.sp
                        else -> 28.sp
                    },
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            TiplyNumericKeypad(
                digitColor = TipSelectionPrimaryTextColor,
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
            .padding(horizontal = 4.dp, vertical = 6.dp),
        color = color,
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

@Composable
private fun TipSelectionTopAppBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 46.dp,
        bottomEnd = 46.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 14.dp,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.64f),
                spotColor = Color.Black.copy(alpha = 0.72f)
            )
            .clip(barShape)
            .background(Color.White)
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 10.dp,
                bottom = 10.dp
            )
    ) {
        TopIcon(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings_back),
                contentDescription = "Назад",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(TipSelectionPrimaryTextColor)
            )
        }
    }
}

@Composable
private fun TopIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
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
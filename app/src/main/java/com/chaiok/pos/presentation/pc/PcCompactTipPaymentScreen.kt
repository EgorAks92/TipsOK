package com.chaiok.pos.presentation.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import kotlin.math.roundToInt

private val PcCompactTipsSectionTopPadding = 28.dp

@Composable
fun PcCompactTipPaymentScreen(
    state: PcCompactTipPaymentUiState,
    onSelectTip: (Int) -> Unit,
    onToggleServiceFee: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071A3A), Color(0xFF0A274F))))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("к оплате", color = Color(0xFFAED4FF), fontFamily = MontserratFontFamily)
                Text(state.amountText, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFontFamily)
            }
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.clickable(onClick = onCancel))
        }

        if (state.isRestartingPayment) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color(0xFF00D4FF), modifier = Modifier.padding(top = 8.dp))
                Text("  Обновляем сумму...", color = Color.White)
            }
        } else {
            val stageHint = when (state.paymentStage) {
                CardPresentingStage.WaitingForCard -> "Можно изменить чаевые до предъявления карты"
                CardPresentingStage.CardDetected,
                CardPresentingStage.Processing,
                CardPresentingStage.PinRequired -> "Оплата выполняется"
                CardPresentingStage.Approved -> "Оплата одобрена"
                else -> null
            }

            if (stageHint != null) {
                Text(
                    text = stageHint,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    fontFamily = MontserratFontFamily,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(PcCompactTipsSectionTopPadding))

        Text("чаевые", color = Color.White, modifier = Modifier.padding(bottom = 10.dp), fontFamily = MontserratFontFamily)
        val tipsEnabled = state.canChangeTips && !state.isRestartingPayment
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(state.availablePercents) { index, percent ->
                val selected = index == state.selectedPercentIndex
                PcCompactTipPresetCard(
                    percentText = "${percent.roundToInt()}%",
                    amountText = formatRubles(state.billAmount * percent / 100.0),
                    selected = selected,
                    enabled = tipsEnabled,
                    onClick = { onSelectTip(index) }
                )
            }
        }

        if (state.serviceFeePercent > 0.0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Возмещение комиссии (${formatRubles(state.serviceFeeAmount)})", color = Color.White)
                Switch(checked = state.isServiceFeeEnabled, onCheckedChange = onToggleServiceFee, enabled = state.canChangeTips)
            }
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage, color = Color(0xFFFF8A8A), modifier = Modifier.padding(top = 12.dp))
            Text("Повторить", color = Color(0xFF7CDFFF), modifier = Modifier.padding(top = 8.dp).clickable(onClick = onRetry))
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
    val shape = RoundedCornerShape(if (selected) 24.dp else 22.dp)
    val backgroundBrush = if (selected) {
        Brush.verticalGradient(listOf(Color(0xFF62E1DC), Color(0xFF1EAFC5)))
    } else {
        Brush.verticalGradient(listOf(Color(0x665A85A8), Color(0x55436586)))
    }

    Box(
        modifier = Modifier
            .size(width = 90.dp, height = if (selected) 140.dp else 124.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = if (selected) 10.dp else 3.dp,
                shape = shape,
                ambientColor = if (selected) Color(0xFF22D3EE).copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f),
                spotColor = if (selected) Color(0xFF22D3EE).copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, Color.White.copy(alpha = if (selected) 0.46f else 0.28f), shape)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
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
                color = Color.White.copy(alpha = if (selected) 0.95f else 0.9f),
                fontSize = 15.sp,
                fontFamily = MontserratFontFamily
            )
            TipPresetIndicator(selected = selected)
        }
    }
}

@Composable
private fun TipPresetIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
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


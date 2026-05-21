package com.chaiok.pos.presentation.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.presentation.theme.MontserratFontFamily

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
        }

        Text("чаевые", color = Color.White, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp), fontFamily = MontserratFontFamily)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(state.availablePercents) { index, percent ->
                val selected = index == state.selectedPercentIndex
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable(enabled = state.canChangeTips) { onSelectTip(index) }
                ) {
                    Column(
                        modifier = Modifier
                            .background(if (selected) Brush.horizontalGradient(listOf(Color(0xFF00C2FF), Color(0xFF00E4B8))) else Brush.verticalGradient(listOf(Color(0x66334A6B), Color(0x66334A6B))))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text("${percent.toInt()}%", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${"%.2f".format(state.billAmount * percent / 100.0)} ₽", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
            }
        }

        if (state.serviceFeePercent > 0.0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Возмещение комиссии (${String.format("%.2f", state.serviceFeeAmount)} ₽)", color = Color.White)
                Switch(checked = state.isServiceFeeEnabled, onCheckedChange = onToggleServiceFee, enabled = state.canChangeTips)
            }
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage, color = Color(0xFFFF8A8A), modifier = Modifier.padding(top = 12.dp))
            Text("Повторить", color = Color(0xFF7CDFFF), modifier = Modifier.padding(top = 8.dp).clickable(onClick = onRetry))
        }
    }
}

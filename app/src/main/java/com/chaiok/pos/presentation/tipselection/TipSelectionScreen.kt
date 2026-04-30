package com.chaiok.pos.presentation.tipselection

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.WaiterProfileCardHeader
import com.chaiok.pos.presentation.theme.MontserratFontFamily

@Composable
fun TipSelectionScreen(state: TipSelectionUiState, onBack: () -> Unit, onPreset: (Int) -> Unit, onCustomStart: () -> Unit, onCustomSet: (String) -> Unit, onDismissCustom: () -> Unit, onPay: () -> Unit, onSnackbarShown: () -> Unit) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snack.showSnackbar(it); onSnackbarShown() } }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        TipSelectionTopAppBar(onBack = onBack)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            WaiterProfileCardHeader(waiterName = state.waiterName, waiterStatus = state.waiterStatus)
            Spacer(Modifier.height(16.dp))
            Text("Выберите чаевые", fontFamily = MontserratFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.availablePercents) { idx, p ->
                    val selected = !state.isCustomSelected && state.selectedPercentIndex == idx
                    val tip = state.calculateTipByPercent(p)
                    Column(Modifier.background(if (selected) Color(0xFF087BE8) else Color(0xFFF4F6FA), RoundedCornerShape(18.dp)).clickable { onPreset(idx) }.padding(16.dp)) {
                        Text("${tip.toInt()} ₽", color = if (selected) Color.White else Color(0xFF1B2128), fontFamily = MontserratFontFamily, fontWeight = FontWeight.Bold)
                        Text("${p.toInt()}%", color = if (selected) Color.White else Color(0xFF68707D), fontFamily = MontserratFontFamily)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().background(if (state.isCustomSelected) Color(0xFFEAF3FF) else Color(0xFFF4F6FA), RoundedCornerShape(18.dp)).clickable { onCustomStart() }.padding(16.dp)) {
                Text(if (state.customTipAmount != null) "Своя сумма: ${state.customTipAmount.toInt()} ₽" else "Своя сумма", fontFamily = MontserratFontFamily)
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onPay, enabled = state.isPayEnabled, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087BE8))) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(text = "Оплатить", fontFamily = MontserratFontFamily, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    AnimatedContent(targetState = state.totalAmount.toInt(), label = "PayAmountAnimation") { amount ->
                        Text(text = "$amount ₽", fontFamily = MontserratFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
            SnackbarHost(snack)
        }
    }

    if (state.showCustomTipDialog) {
        var value by remember { mutableStateOf(state.customTipAmount?.toInt()?.toString().orEmpty()) }
        Dialog(onDismissRequest = onDismissCustom) {
            Column(Modifier.background(Color.White, RoundedCornerShape(20.dp)).padding(16.dp)) {
                Text("Своя сумма")
                OutlinedTextField(value = value, onValueChange = { value = it.filter(Char::isDigit) })
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onCustomSet(value) }) { Text("Сохранить") }
            }
        }
    }
}

@Composable
private fun TipSelectionTopAppBar(onBack: () -> Unit) {
    val barShape = RoundedCornerShape(bottomStart = 46.dp, bottomEnd = 46.dp)
    Box(
        modifier = Modifier.fillMaxWidth().height(72.dp).shadow(
            elevation = 22.dp,
            shape = barShape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.20f),
            spotColor = Color.Black.copy(alpha = 0.28f)
        ).clip(barShape).background(Color.White).padding(start = 32.dp, end = 32.dp, top = 10.dp, bottom = 10.dp)
    ) {
        val interaction = remember { MutableInteractionSource() }
        Box(modifier = Modifier.size(48.dp).align(Alignment.CenterStart).clickable(interactionSource = interaction, indication = null, onClick = onBack), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = R.drawable.ic_settings_back), contentDescription = "Назад", modifier = Modifier.size(30.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(Color(0xFF1B2128)))
        }
    }
}

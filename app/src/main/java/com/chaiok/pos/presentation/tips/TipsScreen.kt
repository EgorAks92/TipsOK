package com.chaiok.pos.presentation.tips

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import java.time.format.DateTimeFormatter

private val TipsBackgroundColor = Color.White
private val TipsPrimaryTextColor = Color(0xFF1B2128)
private val TipsSecondaryTextColor = Color(0xFF69707A)
private val TipsCardColor = Color(0xFFF7F8FA)
private val TipsAccentColor = Color(0xFF087BE8)
private val TipsGreenColor = Color(0xFF14B8A6)

@Composable
fun TipsScreen(
    state: TipsUiState,
    onBack: () -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TipsBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TipsHeader(
                state = state,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "История",
                    color = TipsPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.tips.isEmpty()) {
                    TipsEmptyState(isLoading = state.isLoading)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.tips) { tip ->
                            TipsHistoryItem(
                                date = tip.dateTime.format(dateFormatter),
                                tipAmount = formatMoney(tip.tipAmount),
                                tipPercent = "${tip.tipPercent}%"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TipsHeader(
    state: TipsUiState,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
    ) {
        TipsSummaryCard(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp
                ),
            contentTopPadding = 72.dp
        )

        TiplyBackTopAppBar(
            title = "Мои чаевые",
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}


@Composable
private fun TipsSummaryCard(
    state: TipsUiState,
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF087BE8),
                        Color(0xFF14B8A6)
                    )
                )
            )
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = contentTopPadding,
                bottom = 18.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryColumn(
                title = "Сегодня",
                value = formatMoney(state.summary.todayAmount),
                modifier = Modifier.weight(1f)
            )

            SummaryColumn(
                title = "Записей",
                value = state.summary.count.toString(),
                modifier = Modifier.weight(1f)
            )

            SummaryColumn(
                title = "Средний %",
                value = "${"%.1f".format(state.summary.avgPercent)}%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryColumn(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.82f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = value,
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TipsHistoryItem(
    date: String,
    tipAmount: String,
    tipPercent: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.11f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(TipsCardColor)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = date,
            color = TipsPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        TipsRow(
            title = "Чаевые",
            value = tipAmount,
            valueColor = TipsGreenColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        TipsRow(
            title = "Процент",
            value = tipPercent,
            valueColor = TipsAccentColor
        )
    }
}

@Composable
private fun TipsRow(
    title: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TipsSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 17.sp
        )

        Text(
            text = value,
            color = valueColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TipsEmptyState(
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.11f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(TipsCardColor)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLoading) "Загрузка..." else "Пока нет записей",
            color = TipsSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatMoney(value: Double): String {
    return "${"%.2f".format(value)} ₽"
}

package com.chaiok.pos.presentation.tips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import java.time.format.DateTimeFormatter

private val TipsBackgroundColor = Color.White
private val TipsPrimaryTextColor = Color(0xFF1B2128)
private val TipsSecondaryTextColor = Color(0xFF69707A)
private val TipsCardColor = Color(0xFFF7F8FA)
private val TipsAccentColor = Color(0xFF087BE8)
private val TipsGreenColor = Color(0xFF14B8A6)

private data class TipsLayoutMetrics(
    val isSquareCompact: Boolean,

    val headerHeight: Dp,
    val headerHorizontalPadding: Dp,
    val headerTopPadding: Dp,
    val headerToContentSpacing: Dp,

    val summaryHeight: Dp,
    val summaryCornerRadius: Dp,
    val summaryShadowElevation: Dp,
    val summaryHorizontalPadding: Dp,
    val summaryBottomPadding: Dp,
    val summaryContentTopPadding: Dp,
    val summaryColumnSpacing: Dp,
    val summaryTitleFontSize: TextUnit,
    val summaryTitleLineHeight: TextUnit,
    val summaryValueFontSize: TextUnit,
    val summaryValueLineHeight: TextUnit,

    val contentHorizontalPadding: Dp,
    val historyTitleFontSize: TextUnit,
    val historyTitleLineHeight: TextUnit,
    val historyTitleToListSpacing: Dp,

    val listItemSpacing: Dp,
    val listBottomPadding: Dp,

    val itemCornerRadius: Dp,
    val itemShadowElevation: Dp,
    val itemHorizontalPadding: Dp,
    val itemVerticalPadding: Dp,
    val itemDateFontSize: TextUnit,
    val itemDateLineHeight: TextUnit,
    val itemDateToRowsSpacing: Dp,
    val itemRowsSpacing: Dp,

    val rowTitleFontSize: TextUnit,
    val rowTitleLineHeight: TextUnit,
    val rowValueFontSize: TextUnit,
    val rowValueLineHeight: TextUnit,

    val emptyCornerRadius: Dp,
    val emptyShadowElevation: Dp,
    val emptyHorizontalPadding: Dp,
    val emptyVerticalPadding: Dp,
    val emptyFontSize: TextUnit,
    val emptyLineHeight: TextUnit
)

@Composable
private fun tipsLayoutMetrics(): TipsLayoutMetrics {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp

    return if (isSquareCompact) {
        TipsLayoutMetrics(
            isSquareCompact = true,

            headerHeight = 124.dp,
            headerHorizontalPadding = 12.dp,
            headerTopPadding = 8.dp,
            headerToContentSpacing = 8.dp,

            summaryHeight = 112.dp,
            summaryCornerRadius = 24.dp,
            summaryShadowElevation = 5.dp,
            summaryHorizontalPadding = 12.dp,
            summaryBottomPadding = 10.dp,
            summaryContentTopPadding = 58.dp,
            summaryColumnSpacing = 3.dp,
            summaryTitleFontSize = 10.sp,
            summaryTitleLineHeight = 13.sp,
            summaryValueFontSize = 13.sp,
            summaryValueLineHeight = 16.sp,

            contentHorizontalPadding = 14.dp,
            historyTitleFontSize = 15.sp,
            historyTitleLineHeight = 18.sp,
            historyTitleToListSpacing = 8.dp,

            listItemSpacing = 8.dp,
            listBottomPadding = 10.dp,

            itemCornerRadius = 18.dp,
            itemShadowElevation = 3.dp,
            itemHorizontalPadding = 12.dp,
            itemVerticalPadding = 9.dp,
            itemDateFontSize = 13.sp,
            itemDateLineHeight = 16.sp,
            itemDateToRowsSpacing = 5.dp,
            itemRowsSpacing = 3.dp,

            rowTitleFontSize = 11.sp,
            rowTitleLineHeight = 14.sp,
            rowValueFontSize = 12.sp,
            rowValueLineHeight = 15.sp,

            emptyCornerRadius = 18.dp,
            emptyShadowElevation = 3.dp,
            emptyHorizontalPadding = 16.dp,
            emptyVerticalPadding = 22.dp,
            emptyFontSize = 13.sp,
            emptyLineHeight = 16.sp
        )
    } else {
        TipsLayoutMetrics(
            isSquareCompact = false,

            headerHeight = 178.dp,
            headerHorizontalPadding = 16.dp,
            headerTopPadding = 16.dp,
            headerToContentSpacing = 22.dp,

            summaryHeight = 150.dp,
            summaryCornerRadius = 28.dp,
            summaryShadowElevation = 8.dp,
            summaryHorizontalPadding = 18.dp,
            summaryBottomPadding = 18.dp,
            summaryContentTopPadding = 72.dp,
            summaryColumnSpacing = 6.dp,
            summaryTitleFontSize = 12.sp,
            summaryTitleLineHeight = 15.sp,
            summaryValueFontSize = 17.sp,
            summaryValueLineHeight = 21.sp,

            contentHorizontalPadding = 24.dp,
            historyTitleFontSize = 18.sp,
            historyTitleLineHeight = 22.sp,
            historyTitleToListSpacing = 12.dp,

            listItemSpacing = 12.dp,
            listBottomPadding = 18.dp,

            itemCornerRadius = 24.dp,
            itemShadowElevation = 5.dp,
            itemHorizontalPadding = 16.dp,
            itemVerticalPadding = 14.dp,
            itemDateFontSize = 15.sp,
            itemDateLineHeight = 19.sp,
            itemDateToRowsSpacing = 10.dp,
            itemRowsSpacing = 6.dp,

            rowTitleFontSize = 13.sp,
            rowTitleLineHeight = 17.sp,
            rowValueFontSize = 14.sp,
            rowValueLineHeight = 18.sp,

            emptyCornerRadius = 24.dp,
            emptyShadowElevation = 5.dp,
            emptyHorizontalPadding = 20.dp,
            emptyVerticalPadding = 28.dp,
            emptyFontSize = 15.sp,
            emptyLineHeight = 19.sp
        )
    }
}

@Composable
fun TipsScreen(
    state: TipsUiState,
    onBack: () -> Unit
) {
    val metrics = tipsLayoutMetrics()

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
                metrics = metrics,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(metrics.headerToContentSpacing))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = metrics.contentHorizontalPadding)
            ) {
                Text(
                    text = "История",
                    color = TipsPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.historyTitleFontSize,
                    lineHeight = metrics.historyTitleLineHeight
                )

                Spacer(modifier = Modifier.height(metrics.historyTitleToListSpacing))

                if (state.tips.isEmpty()) {
                    TipsEmptyState(
                        isLoading = state.isLoading,
                        metrics = metrics
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(metrics.listItemSpacing),
                        contentPadding = PaddingValues(bottom = metrics.listBottomPadding),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.tips) { tip ->
                            TipsHistoryItem(
                                date = tip.dateTime.format(dateFormatter),
                                tipAmount = formatMoney(tip.tipAmount),
                                tipPercent = "${tip.tipPercent}%",
                                metrics = metrics
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
    metrics: TipsLayoutMetrics,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.headerHeight)
    ) {
        TipsSummaryCard(
            state = state,
            metrics = metrics,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = metrics.headerHorizontalPadding,
                    end = metrics.headerHorizontalPadding,
                    top = metrics.headerTopPadding
                )
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
    metrics: TipsLayoutMetrics,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.summaryHeight)
            .shadow(
                elevation = metrics.summaryShadowElevation,
                shape = RoundedCornerShape(metrics.summaryCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(metrics.summaryCornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF087BE8),
                        Color(0xFF14B8A6)
                    )
                )
            )
            .padding(
                start = metrics.summaryHorizontalPadding,
                end = metrics.summaryHorizontalPadding,
                top = metrics.summaryContentTopPadding,
                bottom = metrics.summaryBottomPadding
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
                metrics = metrics,
                modifier = Modifier.weight(1f)
            )

            SummaryColumn(
                title = "Записей",
                value = state.summary.count.toString(),
                metrics = metrics,
                modifier = Modifier.weight(1f)
            )

            SummaryColumn(
                title = "Средний %",
                value = "${"%.1f".format(state.summary.avgPercent)}%",
                metrics = metrics,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryColumn(
    title: String,
    value: String,
    metrics: TipsLayoutMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.summaryColumnSpacing)
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.82f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.summaryTitleFontSize,
            lineHeight = metrics.summaryTitleLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.summaryValueFontSize,
            lineHeight = metrics.summaryValueLineHeight,
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
    tipPercent: String,
    metrics: TipsLayoutMetrics
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.itemShadowElevation,
                shape = RoundedCornerShape(metrics.itemCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.11f)
            )
            .clip(RoundedCornerShape(metrics.itemCornerRadius))
            .background(TipsCardColor)
            .padding(
                horizontal = metrics.itemHorizontalPadding,
                vertical = metrics.itemVerticalPadding
            )
    ) {
        Text(
            text = date,
            color = TipsPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.itemDateFontSize,
            lineHeight = metrics.itemDateLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(metrics.itemDateToRowsSpacing))

        TipsRow(
            title = "Чаевые",
            value = tipAmount,
            valueColor = TipsGreenColor,
            metrics = metrics
        )

        Spacer(modifier = Modifier.height(metrics.itemRowsSpacing))

        TipsRow(
            title = "Процент",
            value = tipPercent,
            valueColor = TipsAccentColor,
            metrics = metrics
        )
    }
}

@Composable
private fun TipsRow(
    title: String,
    value: String,
    valueColor: Color,
    metrics: TipsLayoutMetrics
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
            fontSize = metrics.rowTitleFontSize,
            lineHeight = metrics.rowTitleLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            color = valueColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.rowValueFontSize,
            lineHeight = metrics.rowValueLineHeight,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TipsEmptyState(
    isLoading: Boolean,
    metrics: TipsLayoutMetrics
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.emptyShadowElevation,
                shape = RoundedCornerShape(metrics.emptyCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.11f)
            )
            .clip(RoundedCornerShape(metrics.emptyCornerRadius))
            .background(TipsCardColor)
            .padding(
                horizontal = metrics.emptyHorizontalPadding,
                vertical = metrics.emptyVerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLoading) "Загрузка..." else "Пока нет записей",
            color = TipsSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.emptyFontSize,
            lineHeight = metrics.emptyLineHeight,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatMoney(value: Double): String {
    return "${"%.2f".format(value)} ₽"
}
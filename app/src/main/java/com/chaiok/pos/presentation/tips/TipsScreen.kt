package com.chaiok.pos.presentation.tips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import java.time.format.DateTimeFormatter

private val TipsBackgroundColor = Color.White
private val TipsPrimaryTextColor = Color(0xFF1B2128)
private val TipsSecondaryTextColor = Color(0xFF69707A)
private val TipsCardColor = Color(0xFFF7F8FA)
private val TipsAccentColor = Color(0xFF087BE8)
private val TipsGreenColor = Color(0xFF14B8A6)
private val TipsStrokeColor = Color(0xFFE2E7EF)
private val TipsWarningColor = Color(0xFFFFB547)

private data class TipsPremiumMetrics(
    val contentHorizontalPadding: Dp,
    val contentTopPadding: Dp,
    val contentBottomPadding: Dp,

    val headerTitleFontSize: TextUnit,
    val headerTitleLineHeight: TextUnit,
    val headerSubtitleFontSize: TextUnit,
    val headerSubtitleLineHeight: TextUnit,
    val headerToChipsSpacing: Dp,

    val chipHeight: Dp,
    val chipSpacing: Dp,
    val chipCornerRadius: Dp,
    val chipShadowElevation: Dp,
    val chipHorizontalPadding: Dp,
    val chipTitleFontSize: TextUnit,
    val chipTitleLineHeight: TextUnit,
    val chipValueFontSize: TextUnit,
    val chipValueLineHeight: TextUnit,

    val chipsToHistorySpacing: Dp,
    val historyTitleFontSize: TextUnit,
    val historyTitleLineHeight: TextUnit,
    val historyTitleToListSpacing: Dp,

    val listItemSpacing: Dp,
    val listBottomPadding: Dp,

    val itemHeight: Dp,
    val itemCornerRadius: Dp,
    val itemShadowElevation: Dp,
    val itemHorizontalPadding: Dp,
    val itemDateFontSize: TextUnit,
    val itemDateLineHeight: TextUnit,
    val itemAmountFontSize: TextUnit,
    val itemAmountLineHeight: TextUnit,
    val itemPercentFontSize: TextUnit,
    val itemPercentLineHeight: TextUnit,
    val percentBadgeHorizontalPadding: Dp,
    val percentBadgeVerticalPadding: Dp,
    val percentBadgeCornerRadius: Dp,

    val emptyCornerRadius: Dp,
    val emptyShadowElevation: Dp,
    val emptyHorizontalPadding: Dp,
    val emptyVerticalPadding: Dp,
    val emptyFontSize: TextUnit,
    val emptyLineHeight: TextUnit
)

private fun squarePremiumTipsMetrics(): TipsPremiumMetrics {
    return TipsPremiumMetrics(
        contentHorizontalPadding = 16.dp,
        contentTopPadding = 12.dp,
        contentBottomPadding = 10.dp,

        headerTitleFontSize = 18.sp,
        headerTitleLineHeight = 22.sp,
        headerSubtitleFontSize = 11.sp,
        headerSubtitleLineHeight = 15.sp,
        headerToChipsSpacing = 10.dp,

        chipHeight = 56.dp,
        chipSpacing = 7.dp,
        chipCornerRadius = 18.dp,
        chipShadowElevation = 3.dp,
        chipHorizontalPadding = 8.dp,
        chipTitleFontSize = 9.sp,
        chipTitleLineHeight = 12.sp,
        chipValueFontSize = 13.sp,
        chipValueLineHeight = 16.sp,

        chipsToHistorySpacing = 14.dp,
        historyTitleFontSize = 15.sp,
        historyTitleLineHeight = 18.sp,
        historyTitleToListSpacing = 8.dp,

        listItemSpacing = 8.dp,
        listBottomPadding = 8.dp,

        itemHeight = 72.dp,
        itemCornerRadius = 18.dp,
        itemShadowElevation = 3.dp,
        itemHorizontalPadding = 12.dp,
        itemDateFontSize = 12.sp,
        itemDateLineHeight = 15.sp,
        itemAmountFontSize = 15.sp,
        itemAmountLineHeight = 18.sp,
        itemPercentFontSize = 10.sp,
        itemPercentLineHeight = 13.sp,
        percentBadgeHorizontalPadding = 8.dp,
        percentBadgeVerticalPadding = 3.dp,
        percentBadgeCornerRadius = 10.dp,

        emptyCornerRadius = 20.dp,
        emptyShadowElevation = 3.dp,
        emptyHorizontalPadding = 16.dp,
        emptyVerticalPadding = 26.dp,
        emptyFontSize = 13.sp,
        emptyLineHeight = 16.sp
    )
}

@Composable
fun TipsScreen(
    state: TipsUiState,
    onBack: () -> Unit
) {
    when (rememberChaiOkDeviceClass()) {
        ChaiOkDeviceClass.SquareCompact -> {
            TipsSquarePremiumScreen(
                state = state,
                onBack = onBack
            )
        }

        ChaiOkDeviceClass.Regular -> {
            TipsRegularScreen(
                state = state,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun TipsRegularScreen(
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
            TipsRegularHeader(
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
                    TipsRegularEmptyState(isLoading = state.isLoading)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.tips) { tip ->
                            TipsRegularHistoryItem(
                                date = tip.dateTime.format(dateFormatter),
                                tipAmount = formatMoney(tip.tipAmount),
                                tipPercent = "${tip.tipPercent}%",
                                kitchenEvaluation = tip.kitchenEvaluation,
                                serviceEvaluation = tip.serviceEvaluation
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TipsSquarePremiumScreen(
    state: TipsUiState,
    onBack: () -> Unit
) {
    val metrics = squarePremiumTipsMetrics()

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TipsBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TiplyBackTopAppBar(
                title = "Мои чаевые",
                onBack = onBack,
                elevation = 10.dp,
                ambientAlpha = 0.16f,
                spotAlpha = 0.22f
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = metrics.contentHorizontalPadding)
                    .padding(
                        top = metrics.contentTopPadding,
                        bottom = metrics.contentBottomPadding
                    )
            ) {
                Text(
                    text = "Обзор чаевых",
                    color = TipsPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.headerTitleFontSize,
                    lineHeight = metrics.headerTitleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Сегодняшняя смена и последние операции",
                    color = TipsSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = metrics.headerSubtitleFontSize,
                    lineHeight = metrics.headerSubtitleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(metrics.headerToChipsSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.chipSpacing)
                ) {
                    TipsPremiumSummaryChip(
                        title = "Сегодня",
                        value = formatMoney(state.summary.todayAmount),
                        metrics = metrics,
                        modifier = Modifier.weight(1f)
                    )

                    TipsPremiumSummaryChip(
                        title = "Записей",
                        value = state.summary.count.toString(),
                        metrics = metrics,
                        modifier = Modifier.weight(1f)
                    )

                    TipsPremiumSummaryChip(
                        title = "Средний",
                        value = "${"%.1f".format(state.summary.avgPercent)}%",
                        metrics = metrics,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(metrics.chipsToHistorySpacing))

                Text(
                    text = "История",
                    color = TipsPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.historyTitleFontSize,
                    lineHeight = metrics.historyTitleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(metrics.historyTitleToListSpacing))

                if (state.tips.isEmpty()) {
                    TipsPremiumEmptyState(
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
                            TipsPremiumHistoryItem(
                                date = tip.dateTime.format(dateFormatter),
                                tipAmount = formatMoney(tip.tipAmount),
                                tipPercent = "${tip.tipPercent}%",
                                kitchenEvaluation = tip.kitchenEvaluation,
                                serviceEvaluation = tip.serviceEvaluation,
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
private fun TipsPremiumSummaryChip(
    title: String,
    value: String,
    metrics: TipsPremiumMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(metrics.chipHeight)
            .shadow(
                elevation = metrics.chipShadowElevation,
                shape = RoundedCornerShape(metrics.chipCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.045f),
                spotColor = Color.Black.copy(alpha = 0.09f)
            )
            .clip(RoundedCornerShape(metrics.chipCornerRadius))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = TipsStrokeColor.copy(alpha = 0.86f),
                shape = RoundedCornerShape(metrics.chipCornerRadius)
            )
            .padding(horizontal = metrics.chipHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title.uppercase(),
            color = TipsSecondaryTextColor.copy(alpha = 0.82f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.chipTitleFontSize,
            lineHeight = metrics.chipTitleLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            color = TipsPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.chipValueFontSize,
            lineHeight = metrics.chipValueLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TipsRegularHeader(
    state: TipsUiState,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
    ) {
        TipsRegularSummaryCard(
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
private fun TipsRegularSummaryCard(
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
            TipsRegularSummaryColumn(
                title = "Сегодня",
                value = formatMoney(state.summary.todayAmount),
                modifier = Modifier.weight(1f)
            )

            TipsRegularSummaryColumn(
                title = "Записей",
                value = state.summary.count.toString(),
                modifier = Modifier.weight(1f)
            )

            TipsRegularSummaryColumn(
                title = "Средний %",
                value = "${"%.1f".format(state.summary.avgPercent)}%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TipsRegularSummaryColumn(
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
private fun TipsRegularHistoryItem(
    date: String,
    tipAmount: String,
    tipPercent: String,
    kitchenEvaluation: Int?,
    serviceEvaluation: Int?
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

        TipsRegularRow(
            title = "Чаевые",
            value = tipAmount,
            valueColor = TipsGreenColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        TipsRegularRow(
            title = "Процент",
            value = tipPercent,
            valueColor = TipsAccentColor
        )

        kitchenEvaluation.normalizedRating()?.let { rating ->
            Spacer(modifier = Modifier.height(6.dp))
            TipsRegularRow(
                title = "Кухня",
                value = "$rating/5",
                valueColor = TipsWarningColor
            )
        }

        serviceEvaluation.normalizedRating()?.let { rating ->
            Spacer(modifier = Modifier.height(6.dp))
            TipsRegularRow(
                title = "Сервис",
                value = "$rating/5",
                valueColor = TipsWarningColor
            )
        }
    }
}

@Composable
private fun TipsPremiumHistoryItem(
    date: String,
    tipAmount: String,
    tipPercent: String,
    kitchenEvaluation: Int?,
    serviceEvaluation: Int?,
    metrics: TipsPremiumMetrics
) {
    val ratingsText = formatRatingLine(
        kitchen = kitchenEvaluation,
        service = serviceEvaluation
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.itemHeight)
            .shadow(
                elevation = metrics.itemShadowElevation,
                shape = RoundedCornerShape(metrics.itemCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.045f),
                spotColor = Color.Black.copy(alpha = 0.09f)
            )
            .clip(RoundedCornerShape(metrics.itemCornerRadius))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = TipsStrokeColor.copy(alpha = 0.86f),
                shape = RoundedCornerShape(metrics.itemCornerRadius)
            )
            .padding(horizontal = metrics.itemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
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

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(metrics.percentBadgeCornerRadius))
                    .background(TipsAccentColor.copy(alpha = 0.10f))
                    .padding(
                        horizontal = metrics.percentBadgeHorizontalPadding,
                        vertical = metrics.percentBadgeVerticalPadding
                    )
            ) {
                Text(
                    text = tipPercent,
                    color = TipsAccentColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.itemPercentFontSize,
                    lineHeight = metrics.itemPercentLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ratingsText?.let {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = it,
                    color = TipsSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = tipAmount,
            color = TipsGreenColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.itemAmountFontSize,
            lineHeight = metrics.itemAmountLineHeight,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TipsRegularRow(
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
private fun TipsRegularEmptyState(
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

@Composable
private fun TipsPremiumEmptyState(
    isLoading: Boolean,
    metrics: TipsPremiumMetrics
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.emptyShadowElevation,
                shape = RoundedCornerShape(metrics.emptyCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.045f),
                spotColor = Color.Black.copy(alpha = 0.09f)
            )
            .clip(RoundedCornerShape(metrics.emptyCornerRadius))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = TipsStrokeColor.copy(alpha = 0.86f),
                shape = RoundedCornerShape(metrics.emptyCornerRadius)
            )
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

private fun Int?.normalizedRating(): Int? = this?.takeIf { it in 1..5 }

private fun formatRatingLine(kitchen: Int?, service: Int?): String? {
    val items = listOfNotNull(
        kitchen.normalizedRating()?.let { "Кухня ${it}★" },
        service.normalizedRating()?.let { "Сервис ${it}★" }
    )
    return items.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

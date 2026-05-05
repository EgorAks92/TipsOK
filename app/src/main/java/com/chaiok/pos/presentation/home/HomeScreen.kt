package com.chaiok.pos.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.components.WaiterProfileCardHeader
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private data class HomeLayoutMetrics(
    val isSquareCompact: Boolean,
    val amountLabelSize: Int,
    val amountBaseSize: Int,
    val amountSpacer: Dp,
    val amountSectionHeight: Dp,
    val amountBoxHeight: Dp,
    val amountMaxWidth: Dp,
    val topIconSize: Dp,
    val topActionTouchSize: Dp,
    val topBarHeight: Dp,
    val topBarBottomRadius: Dp,
    val topBarHorizontalPadding: Dp,
    val topBarVerticalPadding: Dp,
    val topBarShadowElevation: Dp,
    val logoWidth: Dp,
    val logoHeight: Dp,
    val headerBottomSpacer: Dp,
    val keypadHorizontalPadding: Dp,
    val keypadBottomPadding: Dp,
    val contentBottomPadding: Dp,
    val tableModeHorizontalPadding: Dp,
    val tableModeVerticalPadding: Dp,
    val tableModeFontSize: Int
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onSnackbarShown: () -> Unit
) {
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackState.showSnackbar(it)
            onSnackbarShown()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp
    val isCompactPortrait = screenHeight < 780.dp

    val metrics = when {
        isSquareCompact -> {
            HomeLayoutMetrics(
                isSquareCompact = true,
                amountLabelSize = 12,
                amountBaseSize = 34,
                amountSpacer = 4.dp,
                amountSectionHeight = 58.dp,
                amountBoxHeight = 38.dp,
                amountMaxWidth = 220.dp,
                topIconSize = 22.dp,
                topActionTouchSize = 40.dp,
                topBarHeight = 54.dp,
                topBarBottomRadius = 30.dp,
                topBarHorizontalPadding = 24.dp,
                topBarVerticalPadding = 6.dp,
                topBarShadowElevation = 8.dp,
                logoWidth = 78.dp,
                logoHeight = 26.dp,
                headerBottomSpacer = 6.dp,
                keypadHorizontalPadding = 18.dp,
                keypadBottomPadding = 8.dp,
                contentBottomPadding = 272.dp,
                tableModeHorizontalPadding = 12.dp,
                tableModeVerticalPadding = 14.dp,
                tableModeFontSize = 14
            )
        }

        isCompactPortrait -> {
            HomeLayoutMetrics(
                isSquareCompact = false,
                amountLabelSize = 16,
                amountBaseSize = 48,
                amountSpacer = 12.dp,
                amountSectionHeight = 92.dp,
                amountBoxHeight = 58.dp,
                amountMaxWidth = 320.dp,
                topIconSize = 30.dp,
                topActionTouchSize = 48.dp,
                topBarHeight = 72.dp,
                topBarBottomRadius = 46.dp,
                topBarHorizontalPadding = 32.dp,
                topBarVerticalPadding = 10.dp,
                topBarShadowElevation = 14.dp,
                logoWidth = 100.dp,
                logoHeight = 34.dp,
                headerBottomSpacer = 22.dp,
                keypadHorizontalPadding = 24.dp,
                keypadBottomPadding = 38.dp,
                contentBottomPadding = 390.dp,
                tableModeHorizontalPadding = 8.dp,
                tableModeVerticalPadding = 24.dp,
                tableModeFontSize = 20
            )
        }

        else -> {
            HomeLayoutMetrics(
                isSquareCompact = false,
                amountLabelSize = 16,
                amountBaseSize = 48,
                amountSpacer = 20.dp,
                amountSectionHeight = 92.dp,
                amountBoxHeight = 58.dp,
                amountMaxWidth = 320.dp,
                topIconSize = 34.dp,
                topActionTouchSize = 48.dp,
                topBarHeight = 72.dp,
                topBarBottomRadius = 46.dp,
                topBarHorizontalPadding = 32.dp,
                topBarVerticalPadding = 10.dp,
                topBarShadowElevation = 14.dp,
                logoWidth = 100.dp,
                logoHeight = 34.dp,
                headerBottomSpacer = 22.dp,
                keypadHorizontalPadding = 24.dp,
                keypadBottomPadding = 38.dp,
                contentBottomPadding = 390.dp,
                tableModeHorizontalPadding = 8.dp,
                tableModeVerticalPadding = 24.dp,
                tableModeFontSize = 20
            )
        }
    }

    val firstName = state.profile?.firstName.orEmpty().trim()
    val lastName = state.profile?.lastName.orEmpty().trim()

    val waiterName = listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Ваш официант" }

    val waiterStatus = state.profile?.status
        ?.ifBlank { "Коплю на отпуск!" }
        ?: "Коплю на отпуск!"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (state.settings.tableModeEnabled) 0.dp else metrics.contentBottomPadding)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                WaiterProfileCardHeader(
                    waiterName = waiterName,
                    waiterStatus = waiterStatus,
                    background = state.tileBackground
                )

                HomeTopAppBar(
                    onLogout = onLogout,
                    onOpenSettings = onOpenSettings,
                    metrics = metrics,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Spacer(modifier = Modifier.height(metrics.headerBottomSpacer))

            if (state.settings.tableModeEnabled) {
                Spacer(modifier = Modifier.weight(1f))

                TableModePlaceholder(metrics = metrics)

                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))

                AmountSection(
                    amountInput = state.amountInput,
                    metrics = metrics
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (!state.settings.tableModeEnabled) {
            TiplyNumericKeypad(
                digitColor = Color(0xFF1B2128),
                onDigit = onDigit,
                onDelete = onBackspace,
                onConfirm = onConfirm,
                confirmEnabled = state.amountInput.isNotBlank(),
                isLoading = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = metrics.keypadHorizontalPadding,
                        end = metrics.keypadHorizontalPadding,
                        bottom = metrics.keypadBottomPadding
                    )
            )
        }

        SnackbarHost(
            hostState = snackState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun HomeTopAppBar(
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    metrics: HomeLayoutMetrics,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = metrics.topBarBottomRadius,
        bottomEnd = metrics.topBarBottomRadius
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.topBarHeight)
            .shadow(
                elevation = metrics.topBarShadowElevation,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (metrics.isSquareCompact) 0.32f else 0.64f),
                spotColor = Color.Black.copy(alpha = if (metrics.isSquareCompact) 0.42f else 0.72f)
            )
            .clip(barShape)
            .background(Color.White)
            .padding(
                start = metrics.topBarHorizontalPadding,
                end = metrics.topBarHorizontalPadding,
                top = metrics.topBarVerticalPadding,
                bottom = metrics.topBarVerticalPadding
            )
    ) {
        TopActionIcon(
            onClick = onOpenSettings,
            touchSize = metrics.topActionTouchSize,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home_settings),
                contentDescription = "Настройки",
                modifier = Modifier.size(metrics.topIconSize),
                colorFilter = ColorFilter.tint(Color(0xFF1B2128))
            )
        }

        Image(
            painter = painterResource(id = R.drawable.tiply_logo_black),
            contentDescription = "Tiply",
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = metrics.logoWidth, height = metrics.logoHeight),
            contentScale = ContentScale.Fit
        )

        TopActionIcon(
            onClick = onLogout,
            touchSize = metrics.topActionTouchSize,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home_logout),
                contentDescription = "Выйти",
                modifier = Modifier.size(metrics.topIconSize),
                colorFilter = ColorFilter.tint(Color(0xFF1B2128))
            )
        }
    }
}

@Composable
private fun TopActionIcon(
    onClick: () -> Unit,
    touchSize: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(touchSize)
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

@Composable
private fun AmountSection(
    amountInput: String,
    metrics: HomeLayoutMetrics
) {
    val amountTextSize = when (amountInput.length) {
        in 0..4 -> metrics.amountBaseSize.sp
        in 5..6 -> (metrics.amountBaseSize - if (metrics.isSquareCompact) 3 else 4).sp
        else -> (metrics.amountBaseSize - if (metrics.isSquareCompact) 7 else 10).sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.amountSectionHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Введите сумму счёта:",
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.amountLabelSize.sp,
            lineHeight = (metrics.amountLabelSize + 2).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(metrics.amountSpacer))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.amountBoxHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatAmount(amountInput),
                modifier = Modifier.widthIn(max = metrics.amountMaxWidth),
                color = Color(0xFF1B2128),
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = amountTextSize,
                lineHeight = metrics.amountBaseSize.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TableModePlaceholder(
    metrics: HomeLayoutMetrics
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = metrics.tableModeHorizontalPadding,
                vertical = metrics.tableModeVerticalPadding
            )
            .clip(RoundedCornerShape(if (metrics.isSquareCompact) 22.dp else 30.dp))
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x66263A55),
                            Color(0x55304565),
                            Color(0x44314D70)
                        )
                    )
                )
            }
            .padding(
                horizontal = if (metrics.isSquareCompact) 16.dp else 24.dp,
                vertical = if (metrics.isSquareCompact) 16.dp else 24.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Режим столиков будет настроен позже",
            color = Color.White.copy(alpha = 0.92f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.tableModeFontSize.sp,
            lineHeight = (metrics.tableModeFontSize + 4).sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatAmount(input: String): String {
    if (input.isBlank()) return "₽"

    val grouped = input
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return "$grouped ₽"
}
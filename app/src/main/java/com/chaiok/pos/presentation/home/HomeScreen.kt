package com.chaiok.pos.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.components.WaiterProfileCardHeader
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val HomeBackgroundColor = Color.White
private val HomePrimaryTextColor = Color(0xFF1B2128)
private val HomeSecondaryTextColor = Color(0xFF69707A)
private val HomeAccentColor = Color(0xFF087BE8)
private val HomeGreenColor = Color(0xFF14B8A6)
private val HomeStrokeColor = Color(0xFFE2E7EF)

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
    val keypadTouchSize: Dp?,
    val keypadDigitFontSize: TextUnit?,
    val keypadRowSpacing: Dp?,
    val keypadIconSize: Dp?,

    val contentBottomPadding: Dp,

    val tableModeHorizontalPadding: Dp,
    val tableModeVerticalPadding: Dp,
    val tableModeFontSize: Int
)

private fun squarePremiumHomeMetrics(): HomeLayoutMetrics {
    return HomeLayoutMetrics(
        isSquareCompact = true,

        amountLabelSize = 12,
        amountBaseSize = 38,
        amountSpacer = 2.dp,
        amountSectionHeight = 84.dp,
        amountBoxHeight = 42.dp,
        amountMaxWidth = 260.dp,

        topIconSize = 22.dp,
        topActionTouchSize = 40.dp,
        topBarHeight = 54.dp,
        topBarBottomRadius = 30.dp,
        topBarHorizontalPadding = 24.dp,
        topBarVerticalPadding = 6.dp,
        topBarShadowElevation = 8.dp,
        logoWidth = 78.dp,
        logoHeight = 26.dp,

        headerBottomSpacer = 8.dp,

        keypadHorizontalPadding = 18.dp,
        keypadBottomPadding = 8.dp,
        keypadTouchSize = 56.dp,
        keypadDigitFontSize = 24.sp,
        keypadRowSpacing = 0.dp,
        keypadIconSize = 28.dp,

        contentBottomPadding = 0.dp,

        tableModeHorizontalPadding = 14.dp,
        tableModeVerticalPadding = 14.dp,
        tableModeFontSize = 15
    )
}

private fun regularHomeMetrics(
    isCompactPortrait: Boolean
): HomeLayoutMetrics {
    return if (isCompactPortrait) {
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
            keypadTouchSize = null,
            keypadDigitFontSize = null,
            keypadRowSpacing = null,
            keypadIconSize = null,

            contentBottomPadding = 390.dp,

            tableModeHorizontalPadding = 8.dp,
            tableModeVerticalPadding = 24.dp,
            tableModeFontSize = 20
        )
    } else {
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
            keypadTouchSize = null,
            keypadDigitFontSize = null,
            keypadRowSpacing = null,
            keypadIconSize = null,

            contentBottomPadding = 390.dp,

            tableModeHorizontalPadding = 8.dp,
            tableModeVerticalPadding = 24.dp,
            tableModeFontSize = 20
        )
    }
}

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

    val firstName = state.profile?.firstName.orEmpty().trim()
    val lastName = state.profile?.lastName.orEmpty().trim()

    val waiterName = listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Ваш официант" }

    val waiterStatus = state.profile?.status
        ?.ifBlank { "Коплю на отпуск!" }
        ?: "Коплю на отпуск!"

    when (rememberChaiOkDeviceClass()) {
        ChaiOkDeviceClass.SquareCompact -> {
            HomeSquarePremiumScreen(
                state = state,
                waiterName = waiterName,
                waiterStatus = waiterStatus,
                snackState = snackState,
                onLogout = onLogout,
                onOpenSettings = onOpenSettings,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onConfirm = onConfirm
            )
        }

        ChaiOkDeviceClass.Regular -> {
            HomeRegularScreen(
                state = state,
                waiterName = waiterName,
                waiterStatus = waiterStatus,
                snackState = snackState,
                onLogout = onLogout,
                onOpenSettings = onOpenSettings,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
private fun HomeRegularScreen(
    state: HomeUiState,
    waiterName: String,
    waiterStatus: String,
    snackState: SnackbarHostState,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isCompactPortrait = screenHeight < 780.dp

    val metrics = regularHomeMetrics(
        isCompactPortrait = isCompactPortrait
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (state.settings.tableModeEnabled) {
                        0.dp
                    } else {
                        metrics.contentBottomPadding
                    }
                )
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
            HomeNumericKeypad(
                state = state,
                metrics = metrics,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onConfirm = onConfirm,
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
private fun HomeSquarePremiumScreen(
    state: HomeUiState,
    waiterName: String,
    waiterStatus: String,
    snackState: SnackbarHostState,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit
) {
    val metrics = squarePremiumHomeMetrics()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = metrics.keypadHorizontalPadding)
                        .padding(bottom = metrics.keypadBottomPadding)
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    TableModePlaceholder(metrics = metrics)

                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = metrics.keypadHorizontalPadding)
                        .padding(bottom = metrics.keypadBottomPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PremiumAmountSection(
                        amountInput = state.amountInput,
                        metrics = metrics
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HomeNumericKeypad(
                        state = state,
                        metrics = metrics,
                        onDigit = onDigit,
                        onBackspace = onBackspace,
                        onConfirm = onConfirm,
                        modifier = Modifier.fillMaxWidth()
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
                ambientColor = Color.Black.copy(
                    alpha = if (metrics.isSquareCompact) 0.18f else 0.64f
                ),
                spotColor = Color.Black.copy(
                    alpha = if (metrics.isSquareCompact) 0.24f else 0.72f
                )
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
                colorFilter = ColorFilter.tint(HomePrimaryTextColor)
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
                colorFilter = ColorFilter.tint(HomePrimaryTextColor)
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
            color = HomePrimaryTextColor,
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
                color = HomePrimaryTextColor,
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
private fun PremiumAmountSection(
    amountInput: String,
    metrics: HomeLayoutMetrics
) {
    val amountTextSize = when (amountInput.length) {
        in 0..4 -> metrics.amountBaseSize.sp
        in 5..6 -> (metrics.amountBaseSize - 4).sp
        else -> (metrics.amountBaseSize - 8).sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.amountSectionHeight)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.055f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (amountInput.isNotBlank()) {
                    HomeAccentColor.copy(alpha = 0.22f)
                } else {
                    HomeStrokeColor.copy(alpha = 0.86f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Сумма счёта",
            color = HomeSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.amountLabelSize.sp,
            lineHeight = (metrics.amountLabelSize + 3).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(metrics.amountSpacer))

        Text(
            text = formatAmount(amountInput),
            modifier = Modifier.widthIn(max = metrics.amountMaxWidth),
            color = HomePrimaryTextColor,
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

@Composable
private fun HomeNumericKeypad(
    state: HomeUiState,
    metrics: HomeLayoutMetrics,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    TiplyNumericKeypad(
        digitColor = HomePrimaryTextColor,
        touchSize = metrics.keypadTouchSize,
        digitFontSize = metrics.keypadDigitFontSize,
        rowSpacing = metrics.keypadRowSpacing,
        iconSize = metrics.keypadIconSize,
        onDigit = onDigit,
        onDelete = onBackspace,
        onConfirm = onConfirm,
        confirmEnabled = state.amountInput.isNotBlank(),
        isLoading = false,
        modifier = modifier
    )
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
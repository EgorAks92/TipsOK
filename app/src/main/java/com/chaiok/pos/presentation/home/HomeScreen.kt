package com.chaiok.pos.presentation.home

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import androidx.compose.foundation.layout.widthIn

private data class HomeLayoutMetrics(
    val topRowSpacer: Dp,
    val profileSpacer: Dp,
    val avatarContainerSize: Dp,
    val avatarImageSize: Dp,
    val avatarRadius: Dp,
    val nameSize: Int,
    val nameStatusSpacer: Dp,
    val statusSize: Int,
    val amountLabelSize: Int,
    val amountBaseSize: Int,
    val amountSpacer: Dp,
    val topIconSize: Dp,
    val bottomPadding: Dp,
    val headerHeight: Dp,
    val waiterCardTopPadding: Dp,
    val waiterCardHeight: Dp,
    val waiterCardHorizontalPadding: Dp,
    val waiterCardCutoutPadding: Dp
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onSnackbarShown: () -> Unit,
    onBindCard: () -> Unit,
    onDismissBindDialog: () -> Unit
) {
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackState.showSnackbar(it)
            onSnackbarShown()
        }
    }

    if (state.showLinkCardDialog) {
        LinkCardDialog(
            onBindCard = onBindCard,
            onDismiss = onDismissBindDialog
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val isCompact = screenHeight < 780.dp

        val metrics = if (isCompact) {
            HomeLayoutMetrics(
                topRowSpacer = 6.dp,
                profileSpacer = 0.dp,
                avatarContainerSize = 70.dp,
                avatarImageSize = 62.dp,
                avatarRadius = 22.dp,
                nameSize = 16,
                nameStatusSpacer = 0.dp,
                statusSize = 14,
                amountLabelSize = 16,
                amountBaseSize = 48,
                amountSpacer = 12.dp,
                topIconSize = 30.dp,
                bottomPadding = 12.dp,
                headerHeight = 224.dp,
                waiterCardTopPadding = 16.dp,
                waiterCardHeight = 178.dp,
                waiterCardHorizontalPadding = 32.dp,
                waiterCardCutoutPadding = 12.dp
            )
        } else {
            HomeLayoutMetrics(
                topRowSpacer = 8.dp,
                profileSpacer = 0.dp,
                avatarContainerSize = 72.dp,
                avatarImageSize = 64.dp,
                avatarRadius = 22.dp,
                nameSize = 16,
                nameStatusSpacer = 8.dp,
                statusSize = 14,
                amountLabelSize = 16,
                amountBaseSize = 48,
                amountSpacer = 20.dp,
                topIconSize = 34.dp,
                bottomPadding = 24.dp,
                headerHeight = 242.dp,
                waiterCardTopPadding = 16.dp,
                waiterCardHeight = 196.dp,
                waiterCardHorizontalPadding = 16.dp,
                waiterCardCutoutPadding = 12.dp
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(
                onLogout = onLogout,
                onOpenSettings = onOpenSettings,
                iconSize = metrics.topIconSize,
                metrics = metrics
            )

            Spacer(modifier = Modifier.height(metrics.topRowSpacer))

            ProfileSection(
                state = state,
                metrics = metrics
            )

            if (state.settings.tableModeEnabled) {
                Spacer(modifier = Modifier.weight(1f))
                TableModePlaceholder()
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))

                AmountSection(
                    amountInput = state.amountInput,
                    metrics = metrics
                )

                Spacer(modifier = Modifier.weight(1f))

                TiplyNumericKeypad(
                    digitColor = Color(0xFF1B2128),
                    onDigit = onDigit,
                    onDelete = onBackspace,
                    onConfirm = onConfirm,
                    confirmEnabled = state.amountInput.isNotBlank(),
                    isLoading = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = metrics.bottomPadding)
                )
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
private fun HomeHeader(
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    iconSize: Dp,
    metrics: HomeLayoutMetrics
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.headerHeight)
    ) {
        WaiterBackgroundCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = metrics.waiterCardTopPadding),
            cardHeight = metrics.waiterCardHeight,
            horizontalPadding = metrics.waiterCardHorizontalPadding,
            avatarSize = metrics.avatarContainerSize,
            avatarRadius = metrics.avatarRadius,
            cutoutPadding = metrics.waiterCardCutoutPadding
        )

        HomeTopAppBar(
            onLogout = onLogout,
            onOpenSettings = onOpenSettings,
            iconSize = iconSize,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        WaiterAvatar(
            metrics = metrics,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = metrics.waiterCardTopPadding +
                            metrics.waiterCardHeight -
                            (metrics.avatarContainerSize * 0.64f)
                )
        )
    }
}

@Composable
private fun WaiterBackgroundCard(
    modifier: Modifier = Modifier,
    cardHeight: Dp,
    horizontalPadding: Dp,
    avatarSize: Dp,
    avatarRadius: Dp,
    cutoutPadding: Dp,
    backgroundRes: Int = R.drawable.waiter_card_background
) {
    val cardShape = WaiterCardAvatarCutoutShape(
        cornerRadius = 32.dp,
        avatarSize = avatarSize,
        avatarRadius = avatarRadius,
        cutoutPadding = cutoutPadding
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(cardHeight)
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color(0x40000000),
                spotColor = Color(0x40000000)
            )
            .clip(cardShape)
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

private class WaiterCardAvatarCutoutShape(
    private val cornerRadius: Dp,
    private val avatarSize: Dp,
    private val avatarRadius: Dp,
    private val cutoutPadding: Dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        with(density) {
            val width = size.width
            val height = size.height

            val cardRadius = cornerRadius.toPx()
                .coerceAtMost(width / 2f)
                .coerceAtMost(height / 2f)

            val gap = cutoutPadding.toPx()
            val cutoutWidth = avatarSize.toPx() + gap * 2f
            val cutoutDepth = avatarSize.toPx() * 0.64f + gap
            val cutoutRadius = avatarRadius.toPx() + gap

            val centerX = width / 2f

            val cutoutLeft = (centerX - cutoutWidth / 2f)
                .coerceAtLeast(cardRadius)

            val cutoutRight = (centerX + cutoutWidth / 2f)
                .coerceAtMost(width - cardRadius)

            val cutoutTop = (height - cutoutDepth)
                .coerceAtLeast(cardRadius)

            path.moveTo(cardRadius, 0f)

            path.lineTo(width - cardRadius, 0f)
            path.quadraticBezierTo(width, 0f, width, cardRadius)

            path.lineTo(width, height - cardRadius)
            path.quadraticBezierTo(width, height, width - cardRadius, height)

            val bottomCutoutRadius = 24.dp.toPx()

            path.lineTo(cutoutRight + bottomCutoutRadius, height)

            path.quadraticBezierTo(
                cutoutRight,
                height,
                cutoutRight,
                height - bottomCutoutRadius
            )

            path.lineTo(cutoutRight, cutoutTop + cutoutRadius)

            path.quadraticBezierTo(
                cutoutRight,
                cutoutTop,
                cutoutRight - cutoutRadius,
                cutoutTop
            )

            path.lineTo(cutoutLeft + cutoutRadius, cutoutTop)

            path.quadraticBezierTo(
                cutoutLeft,
                cutoutTop,
                cutoutLeft,
                cutoutTop + cutoutRadius
            )

            path.lineTo(cutoutLeft, height - bottomCutoutRadius)

            path.quadraticBezierTo(
                cutoutLeft,
                height,
                cutoutLeft - bottomCutoutRadius,
                height
            )

            path.lineTo(cardRadius, height)
            path.quadraticBezierTo(0f, height, 0f, height - cardRadius)

            path.lineTo(0f, cardRadius)
            path.quadraticBezierTo(0f, 0f, cardRadius, 0f)

            path.close()
        }

        return Outline.Generic(path)
    }
}

@Composable
private fun WaiterAvatar(
    metrics: HomeLayoutMetrics,
    modifier: Modifier = Modifier
) {
    val avatarShape = RoundedCornerShape(metrics.avatarRadius)

    Box(
        modifier = modifier
            .size(metrics.avatarContainerSize)
            .shadow(
                elevation = 8.dp,
                shape = avatarShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(avatarShape)
            .background(Color(0xFFF2F3F2)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_waiter_avatar),
            contentDescription = "Аватар официанта",
            modifier = Modifier.size(metrics.avatarImageSize),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun LinkCardDialog(
    onBindCard: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(Color.White)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 22.dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tiply_logo_black),
                    contentDescription = "Tiply",
                    modifier = Modifier.size(width = 100.dp, height = 34.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Карта не привязана!",
                    color = Color(0xFF18212B),
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "К вашему профилю не привязана\nбанковская карта. Привяжите карту,\nчтобы получать чаевые.",
                    color = Color(0xFF1E2530),
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DialogActionText(
                        text = "Позже",
                        color = Color(0xFFFF4545),
                        onClick = onDismiss
                    )

                    DialogActionText(
                        text = "Привязать карту",
                        color = Color(0xFF087BE8),
                        onClick = onBindCard
                    )
                }
            }
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
private fun HomeTopAppBar(
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    iconSize: Dp,
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
            .height(84.dp)
            .shadow(
                elevation = 22.dp,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.20f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(barShape)
            .background(Color.White)
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 14.dp,
                bottom = 12.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopActionIcon(onClick = onOpenSettings) {
                Image(
                    painter = painterResource(id = R.drawable.ic_home_settings),
                    contentDescription = "Настройки",
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(Color(0xFF1B2128))
                )
            }

            TopActionIcon(onClick = onLogout) {
                Image(
                    painter = painterResource(id = R.drawable.ic_home_logout),
                    contentDescription = "Выйти",
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(Color(0xFF1B2128))
                )
            }
        }
    }
}

@Composable
private fun TopActionIcon(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
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

@Composable
private fun ProfileSection(
    state: HomeUiState,
    metrics: HomeLayoutMetrics
) {
    val firstName = state.profile?.firstName.orEmpty().trim()
    val lastName = state.profile?.lastName.orEmpty().trim()

    val displayName = listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Ваш официант" }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(metrics.profileSpacer))

        Text(
            text = displayName,
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.nameSize.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(metrics.nameStatusSpacer))

        Text(
            text = state.profile?.status?.ifBlank { "Коплю на отпуск!" } ?: "Коплю на отпуск!",
            color = Color(0xFF3B4148),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.statusSize.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AmountSection(
    amountInput: String,
    metrics: HomeLayoutMetrics
) {
    val amountTextSize = when (amountInput.length) {
        in 0..4 -> metrics.amountBaseSize.sp
        in 5..6 -> (metrics.amountBaseSize - 4).sp
        else -> (metrics.amountBaseSize - 10).sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Введите сумму счёта:",
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.amountLabelSize.sp,
            lineHeight = metrics.amountLabelSize.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(metrics.amountSpacer))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatAmount(amountInput),
                modifier = Modifier.widthIn(max = 320.dp),
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
private fun TableModePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(30.dp))
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
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Режим столиков будет настроен позже",
            color = Color.White.copy(alpha = 0.92f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatAmount(input: String): String {
    if (input.isBlank()) return "₽"
    val grouped = input.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₽"
}
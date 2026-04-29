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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private data class HomeLayoutMetrics(
    val topRowSpacer: Dp,
    val profileSpacer: Dp,
    val headerHeight: Dp,
    val topBarHeight: Dp,
    val waiterCardHeight: Dp,
    val waiterCardTopPadding: Dp,
    val waiterCardHorizontalInset: Dp,
    val waiterCardCornerRadius: Dp,
    val cardCutoutRadius: Dp,
    val cardCutoutDepth: Dp,
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
    val bottomPadding: Dp
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
            .background(Color(0xFFF4F4F2))
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val isCompact = screenHeight < 780.dp

        val metrics = if (isCompact) {
            HomeLayoutMetrics(
                topRowSpacer = 6.dp,
                profileSpacer = 10.dp,
                headerHeight = 300.dp,
                topBarHeight = 108.dp,
                waiterCardHeight = 232.dp,
                waiterCardTopPadding = 52.dp,
                waiterCardHorizontalInset = 8.dp,
                waiterCardCornerRadius = 34.dp,
                cardCutoutRadius = 36.dp,
                cardCutoutDepth = 56.dp,
                avatarContainerSize = 72.dp,
                avatarImageSize = 64.dp,
                avatarRadius = 22.dp,
                nameSize = 16,
                nameStatusSpacer = 8.dp,
                statusSize = 14,
                amountLabelSize = 16,
                amountBaseSize = 48,
                amountSpacer = 12.dp,
                topIconSize = 30.dp,
                bottomPadding = 12.dp
            )
        } else {
            HomeLayoutMetrics(
                topRowSpacer = 8.dp,
                profileSpacer = 16.dp,
                headerHeight = 320.dp,
                topBarHeight = 116.dp,
                waiterCardHeight = 248.dp,
                waiterCardTopPadding = 56.dp,
                waiterCardHorizontalInset = 8.dp,
                waiterCardCornerRadius = 36.dp,
                cardCutoutRadius = 38.dp,
                cardCutoutDepth = 60.dp,
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
                bottomPadding = 24.dp
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
                    modifier = Modifier.size(width = 116.dp, height = 38.dp),
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.waiterCardHeight)
                .padding(
                    top = metrics.waiterCardTopPadding,
                    start = metrics.waiterCardHorizontalInset,
                    end = metrics.waiterCardHorizontalInset
                )
                .clip(
                    WaiterCardShape(
                        cornerRadius = metrics.waiterCardCornerRadius,
                        cutoutRadius = metrics.cardCutoutRadius,
                        cutoutDepth = metrics.cardCutoutDepth
                    )
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFDFEFF), Color(0xFFF3F6FA), Color(0xFFF9FBFE))
                    )
                )
                .drawBehind {
                    val accentBlue = Color(0x3327C7F7)
                    val softCyan = Color(0x2B49E4FF)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3D58DAEC), Color.Transparent),
                            center = center.copy(x = size.width * 0.2f, y = size.height * 0.35f),
                            radius = size.minDimension * 0.75f
                        ),
                        radius = size.minDimension * 0.75f,
                        center = center.copy(x = size.width * 0.2f, y = size.height * 0.35f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3049B9FF), Color.Transparent),
                            center = center.copy(x = size.width * 0.85f, y = size.height * 0.85f),
                            radius = size.minDimension * 0.65f
                        ),
                        radius = size.minDimension * 0.65f,
                        center = center.copy(x = size.width * 0.85f, y = size.height * 0.85f)
                    )
                    val firstCurve = Path().apply {
                        moveTo(-size.width * 0.15f, size.height * 0.9f)
                        cubicTo(
                            size.width * 0.22f, size.height * 0.88f,
                            size.width * 0.4f, size.height * 0.25f,
                            size.width * 0.78f, -size.height * 0.08f
                        )
                    }
                    drawPath(
                        path = firstCurve,
                        color = accentBlue,
                        style = Stroke(width = 2.6f)
                    )

                    val secondCurve = Path().apply {
                        moveTo(size.width * 0.08f, size.height * 1.02f)
                        cubicTo(
                            size.width * 0.32f, size.height * 0.78f,
                            size.width * 0.66f, size.height * 0.34f,
                            size.width * 1.02f, size.height * 0.2f
                        )
                    }
                    drawPath(
                        path = secondCurve,
                        color = softCyan,
                        style = Stroke(width = 1.8f)
                    )
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp, 0.dp, 42.dp, 42.dp))
                .shadow(14.dp, RoundedCornerShape(0.dp, 0.dp, 42.dp, 42.dp))
                .background(Color.White)
                .height(metrics.topBarHeight)
                .padding(top = 8.dp, start = 6.dp, end = 6.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopActionIcon(onClick = onLogout) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_home_logout),
                        contentDescription = "Выйти",
                        modifier = Modifier.size(iconSize),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF1B2128))
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.tiply_logo),
                    contentDescription = "Tiply",
                    modifier = Modifier.size(width = 110.dp, height = 36.dp),
                    contentScale = ContentScale.Fit
                )

                TopActionIcon(onClick = onOpenSettings) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_home_settings),
                        contentDescription = "Настройки",
                        modifier = Modifier.size(iconSize),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF1B2128))
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(metrics.avatarContainerSize)
                .shadow(12.dp, RoundedCornerShape(metrics.avatarRadius))
                .clip(RoundedCornerShape(metrics.avatarRadius))
                .background(Color(0xFFF7F8FA)),
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
}

private class WaiterCardShape(
    private val cornerRadius: Dp,
    private val cutoutRadius: Dp,
    private val cutoutDepth: Dp
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): androidx.compose.ui.graphics.Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }
        val cutoutDepthPx = with(density) { cutoutDepth.toPx() }
        val centerX = size.width / 2f
        val leftCut = centerX - cutoutRadiusPx
        val rightCut = centerX + cutoutRadiusPx

        val path = Path().apply {
            moveTo(radiusPx, 0f)
            lineTo(size.width - radiusPx, 0f)
            quadraticBezierTo(size.width, 0f, size.width, radiusPx)
            lineTo(size.width, size.height - radiusPx)
            quadraticBezierTo(size.width, size.height, size.width - radiusPx, size.height)
            lineTo(rightCut, size.height)
            quadraticBezierTo(centerX, size.height, centerX, size.height - cutoutDepthPx)
            quadraticBezierTo(centerX, size.height, leftCut, size.height)
            lineTo(radiusPx, size.height)
            quadraticBezierTo(0f, size.height, 0f, size.height - radiusPx)
            lineTo(0f, radiusPx)
            quadraticBezierTo(0f, 0f, radiusPx, 0f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
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
        Spacer(modifier = Modifier.height(metrics.profileSpacer + 8.dp))

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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Введите сумму счёта:",
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.amountLabelSize.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(metrics.amountSpacer))

        Text(
            text = formatAmount(amountInput),
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = amountTextSize,
            textAlign = TextAlign.Center,
            lineHeight = amountTextSize,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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

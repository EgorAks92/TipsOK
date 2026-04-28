package com.chaiok.pos.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private data class HomeLayoutMetrics(
    val topRowSpacer: Dp,
    val cardHeight: Dp,
    val cardRadius: Dp,
    val avatarSize: Dp,
    val avatarFontSize: Int,
    val avatarOverlap: Dp,
    val afterProfileSpacer: Dp,
    val nameSize: Int,
    val statusSize: Int,
    val amountLabelSize: Int,
    val amountBaseSize: Int,
    val amountSpacer: Dp,
    val beforeKeypadSpacer: Dp,
    val keypadTouchSize: Dp,
    val keypadDigitSize: Int,
    val keypadRowSpacing: Dp,
    val keypadIconSize: Dp,
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
        AlertDialog(
            onDismissRequest = onDismissBindDialog,
            containerColor = Color(0xFF151B23),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = "Карта не привязана",
                    fontFamily = MontserratFontFamily
                )
            },
            text = {
                Text(
                    text = "К вашему профилю не привязана банковская карта. Привяжите карту, чтобы получать чаевые.",
                    fontFamily = MontserratFontFamily,
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                TextButton(onClick = onBindCard) {
                    Text(text = "Привязать карту", color = Color(0xFF20E3DE), fontFamily = MontserratFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBindDialog) {
                    Text(text = "Позже", color = Color(0xFF0791E6), fontFamily = MontserratFontFamily)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawHomeBackground() }
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxHeight < 780.dp
            val metrics = if (isCompact) {
                HomeLayoutMetrics(
                    topRowSpacer = 8.dp,
                    cardHeight = 122.dp,
                    cardRadius = 32.dp,
                    avatarSize = 72.dp,
                    avatarFontSize = 68,
                    avatarOverlap = 30.dp,
                    afterProfileSpacer = 24.dp,
                    nameSize = 23,
                    statusSize = 18,
                    amountLabelSize = 16,
                    amountBaseSize = 44,
                    amountSpacer = 6.dp,
                    beforeKeypadSpacer = 12.dp,
                    keypadTouchSize = 74.dp,
                    keypadDigitSize = 48,
                    keypadRowSpacing = 10.dp,
                    keypadIconSize = 58.dp,
                    topIconSize = 30.dp,
                    bottomPadding = 12.dp
                )
            } else {
                HomeLayoutMetrics(
                    topRowSpacer = 12.dp,
                    cardHeight = 142.dp,
                    cardRadius = 32.dp,
                    avatarSize = 82.dp,
                    avatarFontSize = 78,
                    avatarOverlap = 36.dp,
                    afterProfileSpacer = 30.dp,
                    nameSize = 25,
                    statusSize = 20,
                    amountLabelSize = 17,
                    amountBaseSize = 52,
                    amountSpacer = 8.dp,
                    beforeKeypadSpacer = 16.dp,
                    keypadTouchSize = 82.dp,
                    keypadDigitSize = 54,
                    keypadRowSpacing = 14.dp,
                    keypadIconSize = 62.dp,
                    topIconSize = 34.dp,
                    bottomPadding = 16.dp
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                TopActionRow(
                    onLogout = onLogout,
                    onOpenSettings = onOpenSettings,
                    iconSize = metrics.topIconSize
                )

                Spacer(modifier = Modifier.height(metrics.topRowSpacer))

                ProfileSection(state = state, metrics = metrics)

                if (state.settings.tableModeEnabled) {
                    TableModePlaceholder()
                } else {
                    Spacer(modifier = Modifier.height(metrics.afterProfileSpacer))
                    AmountSection(amountInput = state.amountInput, metrics = metrics)
                    Spacer(modifier = Modifier.height(metrics.beforeKeypadSpacer))
                    HomeKeypad(
                        onDigit = onDigit,
                        onDelete = onBackspace,
                        onConfirm = onConfirm,
                        touchSize = metrics.keypadTouchSize,
                        digitSize = metrics.keypadDigitSize,
                        rowSpacing = metrics.keypadRowSpacing,
                        iconSize = metrics.keypadIconSize,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(metrics.bottomPadding))
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
private fun TopActionRow(
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    iconSize: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopActionIcon(onClick = onLogout) {
            Icon(
                imageVector = Icons.Outlined.ExitToApp,
                contentDescription = "Выйти",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }

        TopActionIcon(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Настройки",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun TopActionIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
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
private fun ProfileSection(state: HomeUiState, metrics: HomeLayoutMetrics) {
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
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.cardHeight)
                    .clip(RoundedCornerShape(metrics.cardRadius))
                    .drawBehind { drawProfileCardBackground(metrics.cardRadius) }
            )

            Box(
                modifier = Modifier
                    .size(metrics.avatarSize)
                    .offset(y = metrics.avatarOverlap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👨‍💼",
                    fontSize = metrics.avatarFontSize.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(metrics.avatarOverlap + 6.dp))

        Text(
            text = displayName,
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.nameSize.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = state.profile?.status?.ifBlank { "Коплю на отпуск!" } ?: "Коплю на отпуск!",
            color = Color.White.copy(alpha = 0.9f),
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
private fun AmountSection(amountInput: String, metrics: HomeLayoutMetrics) {
    val amountTextSize = when (amountInput.length) {
        in 0..4 -> metrics.amountBaseSize.sp
        in 5..6 -> (metrics.amountBaseSize - 4).sp
        else -> (metrics.amountBaseSize - 8).sp
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Введите сумму счёта",
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.amountLabelSize.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(metrics.amountSpacer))

        Text(
            text = formatAmount(amountInput),
            color = Color.White,
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
                        listOf(Color(0x66263A55), Color(0x55304565), Color(0x44314D70))
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

@Composable
private fun HomeKeypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    touchSize: Dp,
    digitSize: Int,
    rowSpacing: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { label ->
                    HomeKeypadDigit(
                        label = label,
                        onClick = { onDigit(label) },
                        touchSize = touchSize,
                        digitSize = digitSize
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeKeypadIconButton(
                iconRes = R.drawable.ic_keypad_delete,
                contentDescription = "Удалить",
                onClick = onDelete,
                touchSize = touchSize,
                iconSize = iconSize
            )

            HomeKeypadDigit(
                label = "0",
                onClick = { onDigit("0") },
                touchSize = touchSize,
                digitSize = digitSize
            )

            HomeKeypadIconButton(
                iconRes = R.drawable.ic_keypad_confirm,
                contentDescription = "Подтвердить",
                onClick = onConfirm,
                touchSize = touchSize,
                iconSize = iconSize
            )
        }
    }
}

@Composable
private fun HomeKeypadDigit(
    label: String,
    onClick: () -> Unit,
    touchSize: Dp,
    digitSize: Int
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(touchSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = digitSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFontFamily,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeKeypadIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    touchSize: Dp,
    iconSize: Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(touchSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun DrawScope.drawHomeBackground() {
    val baseTop = Color(0xFF151B23)
    val baseBottom = Color(0xFF111821)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(baseTop, baseBottom)
        )
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x660791E6),
                Color(0x4D176FC6),
                Color.Transparent
            ),
            center = Offset(size.width * 0.2f, size.height * 0.72f),
            radius = size.maxDimension * 0.95f
        ),
        radius = size.maxDimension
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x2D2A78B9),
                Color.Transparent
            ),
            center = Offset(size.width * 0.82f, size.height * 0.7f),
            radius = size.maxDimension * 0.78f
        ),
        radius = size.maxDimension
    )
}

private fun DrawScope.drawProfileCardBackground(radius: Dp) {
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF08122A),
                Color(0xFF0B2E54),
                Color(0xFF0D7C9C)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx(), radius.toPx())
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x770791E6), Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.14f),
            radius = size.width * 0.62f
        ),
        radius = size.width * 0.62f,
        center = Offset(size.width * 0.12f, size.height * 0.14f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x7A1DE9E7), Color.Transparent),
            center = Offset(size.width * 0.92f, size.height * 0.32f),
            radius = size.width * 0.72f
        ),
        radius = size.width * 0.72f,
        center = Offset(size.width * 0.92f, size.height * 0.32f)
    )
}

private fun formatAmount(input: String): String {
    if (input.isBlank()) return "₽"
    val grouped = input.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₽"
}

package com.chaiok.pos.presentation.home

import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
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
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private data class HomeLayoutMetrics(
    val topRowSpacer: Dp,
    val profileSpacer: Dp,
    val avatarContainerSize: Dp,
    val avatarImageSize: Dp,
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
                    Text(
                        text = "Привязать карту",
                        color = Color(0xFF20E3DE),
                        fontFamily = MontserratFontFamily
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBindDialog) {
                    Text(
                        text = "Позже",
                        color = Color(0xFF0791E6),
                        fontFamily = MontserratFontFamily
                    )
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
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val isCompact = screenHeight < 780.dp

        val metrics = if (isCompact) {
            HomeLayoutMetrics(
                topRowSpacer = 6.dp,
                profileSpacer = 10.dp,
                avatarContainerSize = 72.dp,
                avatarImageSize = 64.dp,
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
                avatarContainerSize = 72.dp,
                avatarImageSize = 64.dp,
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
            TopActionRow(
                onLogout = onLogout,
                onOpenSettings = onOpenSettings,
                iconSize = metrics.topIconSize
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
                    onDigit = onDigit,
                    onDelete = onBackspace,
                    onConfirm = onConfirm,
                    confirmEnabled = true,
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
            Image(
                painter = painterResource(id = R.drawable.ic_home_logout),
                contentDescription = "Выйти",
                modifier = Modifier.size(iconSize)
            )
        }

        TopActionIcon(onClick = onOpenSettings) {
            Image(
                painter = painterResource(id = R.drawable.ic_home_settings),
                contentDescription = "Настройки",
                modifier = Modifier.size(iconSize)
            )
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
        Box(
            modifier = Modifier
                .size(metrics.avatarContainerSize)
                .clip(RoundedCornerShape(percent = 50))
                .drawBehind { drawCircle(Color.White) },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_waiter_avatar),
                contentDescription = "Аватар официанта",
                modifier = Modifier.size(metrics.avatarImageSize),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(metrics.profileSpacer))

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

        Spacer(modifier = Modifier.height(metrics.nameStatusSpacer))

        Text(
            text = state.profile?.status?.ifBlank { "Коплю на отпуск!" } ?: "Коплю на отпуск!",
            color = Color.White.copy(alpha = 0.88f),
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
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
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

private fun formatAmount(input: String): String {
    if (input.isBlank()) return "₽"
    val grouped = input.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₽"
}
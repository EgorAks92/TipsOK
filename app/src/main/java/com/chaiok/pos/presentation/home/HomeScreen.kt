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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

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
        Column(modifier = Modifier.fillMaxSize()) {
            TopActionRow(onLogout = onLogout, onOpenSettings = onOpenSettings)

            Spacer(modifier = Modifier.height(20.dp))

            ProfileSection(state = state)

            Spacer(modifier = Modifier.height(34.dp))

            if (state.settings.tableModeEnabled) {
                TableModePlaceholder()
            } else {
                AmountSection(amountInput = state.amountInput)
                Spacer(modifier = Modifier.weight(1f))
                HomeKeypad(
                    onDigit = onDigit,
                    onDelete = onBackspace,
                    onConfirm = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
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
private fun TopActionRow(onLogout: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopActionIcon(onClick = onLogout) {
            Icon(
                imageVector = Icons.Outlined.ExitToApp,
                contentDescription = "Выйти",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        TopActionIcon(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Настройки",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
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
private fun ProfileSection(state: HomeUiState) {
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
                    .height(180.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .drawBehind { drawProfileCardBackground() }
            )

            Text(
                text = "👨‍💼",
                fontSize = 86.sp,
                modifier = Modifier.offset(y = 44.dp)
            )
        }

        Spacer(modifier = Modifier.height(46.dp))

        Text(
            text = displayName,
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = state.profile?.status?.ifBlank { "Коплю на отпуск!" } ?: "Коплю на отпуск!",
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AmountSection(amountInput: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Введите сумму счёта:",
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = formatAmount(amountInput),
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 58.sp,
            textAlign = TextAlign.Center,
            lineHeight = 58.sp
        )
    }
}

@Composable
private fun TableModePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 32.dp)
            .clip(RoundedCornerShape(30.dp))
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x66263A55), Color(0x55304565), Color(0x44314D70))
                    )
                )
            }
            .padding(horizontal = 24.dp, vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Режим столиков будет настроен позже",
            color = Color.White.copy(alpha = 0.92f),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 23.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeKeypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { label ->
                    HomeKeypadDigit(label = label) { onDigit(label) }
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
                touchSize = 86.dp,
                iconSize = 66.dp
            )

            HomeKeypadDigit(label = "0") { onDigit("0") }

            HomeKeypadIconButton(
                iconRes = R.drawable.ic_keypad_confirm,
                contentDescription = "Подтвердить",
                onClick = onConfirm,
                touchSize = 86.dp,
                iconSize = 66.dp
            )
        }
    }
}

@Composable
private fun HomeKeypadDigit(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(86.dp)
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
            fontSize = 56.sp,
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
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF151B23),
                Color(0xFF111821)
            )
        )
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x660791E6),
                Color(0x4D176FC6),
                Color.Transparent
            ),
            center = Offset(size.width * 0.24f, size.height * 0.72f),
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

private fun DrawScope.drawProfileCardBackground() {
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
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(36.dp.toPx(), 36.dp.toPx())
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

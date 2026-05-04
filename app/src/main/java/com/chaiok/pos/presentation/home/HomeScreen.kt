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
    onSnackbarShown: () -> Unit
) {
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackState.showSnackbar(it)
            onSnackbarShown()
        }
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
                amountLabelSize = 16,
                amountBaseSize = 48,
                amountSpacer = 12.dp,
                topIconSize = 30.dp,
                bottomPadding = 24.dp
            )
        } else {
            HomeLayoutMetrics(
                amountLabelSize = 16,
                amountBaseSize = 48,
                amountSpacer = 20.dp,
                topIconSize = 34.dp,
                bottomPadding = 24.dp
            )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 390.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                WaiterProfileCardHeader(
                    waiterName = waiterName,
                    waiterStatus = waiterStatus
                )

                HomeTopAppBar(
                    onLogout = onLogout,
                    onOpenSettings = onOpenSettings,
                    iconSize = metrics.topIconSize,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

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
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 38.dp
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
            .height(72.dp)
            .shadow(
                elevation = 14.dp,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.64f),
                spotColor = Color.Black.copy(alpha = 0.72f)
            )
            .clip(barShape)
            .background(Color.White)
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 10.dp,
                bottom = 10.dp
            )
    ) {
        TopActionIcon(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home_settings),
                contentDescription = "Настройки",
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(Color(0xFF1B2128))
            )
        }

        Image(
            painter = painterResource(id = R.drawable.tiply_logo_black),
            contentDescription = "Tiply",
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 100.dp, height = 34.dp),
            contentScale = ContentScale.Fit
        )

        TopActionIcon(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home_logout),
                contentDescription = "Выйти",
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(Color(0xFF1B2128))
            )
        }
    }
}

@Composable
private fun TopActionIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
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
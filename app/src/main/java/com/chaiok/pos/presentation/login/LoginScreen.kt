package com.chaiok.pos.presentation.login

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val LightScreenColor = Color(0xFFFFFFFF)
private val PrimaryTextColor = Color(0xFF1B2128)

private data class LoginLayoutMetrics(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val closeTouchSize: Dp,
    val closeIconSize: Dp,
    val closeBottomSpacer: Dp,
    val logoWidth: Dp,
    val logoHeight: Dp,
    val logoTitleSpacer: Dp,
    val titleSize: Int,
    val titleLineHeight: Int,
    val titleDotsSpacer: Dp,
    val dotSize: Dp,
    val dotPadding: Dp,
    val dotSpacing: Dp,
    val dotShadowFilled: Dp,
    val dotShadowEmpty: Dp,
    val keypadBottomPadding: Dp,
    val keypadTouchSize: Dp,
    val keypadIconSize: Dp,
    val keypadDigitSize: Int
)

@Composable
fun LoginScreen(
    state: LoginUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onLogin: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp
    val isCompactPortrait = screenHeight < 780.dp

    val metrics = when {
        isSquareCompact -> {
            LoginLayoutMetrics(
                horizontalPadding = 18.dp,
                verticalPadding = 8.dp,
                closeTouchSize = 40.dp,
                closeIconSize = 26.dp,
                closeBottomSpacer = 2.dp,
                logoWidth = 86.dp,
                logoHeight = 34.dp,
                logoTitleSpacer = 22.dp,
                titleSize = 14,
                titleLineHeight = 18,
                titleDotsSpacer = 10.dp,
                dotSize = 30.dp,
                dotPadding = 4.dp,
                dotSpacing = 12.dp,
                dotShadowFilled = 8.dp,
                dotShadowEmpty = 6.dp,
                keypadBottomPadding = 4.dp,
                keypadTouchSize = 52.dp,
                keypadIconSize = 26.dp,
                keypadDigitSize = 23
            )
        }

        isCompactPortrait -> {
            LoginLayoutMetrics(
                horizontalPadding = 24.dp,
                verticalPadding = 14.dp,
                closeTouchSize = 48.dp,
                closeIconSize = 34.dp,
                closeBottomSpacer = 28.dp,
                logoWidth = 120.dp,
                logoHeight = 54.dp,
                logoTitleSpacer = 116.dp,
                titleSize = 16,
                titleLineHeight = 31,
                titleDotsSpacer = 32.dp,
                dotSize = 46.dp,
                dotPadding = 6.dp,
                dotSpacing = 18.dp,
                dotShadowFilled = 12.dp,
                dotShadowEmpty = 9.dp,
                keypadBottomPadding = 24.dp,
                keypadTouchSize = 78.dp,
                keypadIconSize = 36.dp,
                keypadDigitSize = 30
            )
        }

        else -> {
            LoginLayoutMetrics(
                horizontalPadding = 24.dp,
                verticalPadding = 14.dp,
                closeTouchSize = 48.dp,
                closeIconSize = 34.dp,
                closeBottomSpacer = 28.dp,
                logoWidth = 120.dp,
                logoHeight = 54.dp,
                logoTitleSpacer = 116.dp,
                titleSize = 16,
                titleLineHeight = 31,
                titleDotsSpacer = 32.dp,
                dotSize = 46.dp,
                dotPadding = 6.dp,
                dotSpacing = 18.dp,
                dotShadowFilled = 12.dp,
                dotShadowEmpty = 9.dp,
                keypadBottomPadding = 24.dp,
                keypadTouchSize = 86.dp,
                keypadIconSize = 38.dp,
                keypadDigitSize = 32
            )
        }
    }

    val hasError = state.errorMessage != null

    var submittedPin by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(
        state.pin,
        state.isLoading
    ) {
        if (state.pin.length < PIN_LENGTH) {
            submittedPin = null
        }

        if (
            state.pin.length == PIN_LENGTH &&
            !state.isLoading &&
            submittedPin != state.pin
        ) {
            submittedPin = state.pin
            onLogin()
        }
    }

    val shake = remember { Animatable(0f) }

    LaunchedEffect(state.triggerShake) {
        if (state.triggerShake > 0) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    -12f at 60
                    12f at 120
                    -10f at 180
                    10f at 240
                    0f at 350
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightScreenColor)
            .padding(
                horizontal = metrics.horizontalPadding,
                vertical = metrics.verticalPadding
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val closeInteraction = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .size(metrics.closeTouchSize)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = {
                                onClose?.invoke() ?: (context as? Activity)?.finish()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Закрыть",
                        modifier = Modifier.size(metrics.closeIconSize),
                        contentScale = ContentScale.Fit,
                        alpha = 0.95f
                    )
                }
            }

            Spacer(modifier = Modifier.height(metrics.closeBottomSpacer))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tiply_logo_black),
                    contentDescription = "Tiply",
                    modifier = Modifier.size(
                        width = metrics.logoWidth,
                        height = metrics.logoHeight
                    ),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(metrics.logoTitleSpacer))

                Text(
                    text = "Введите PIN-код",
                    color = PrimaryTextColor,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MontserratFontFamily,
                        fontSize = metrics.titleSize.sp,
                        lineHeight = metrics.titleLineHeight.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(metrics.titleDotsSpacer))

                PinDots(
                    pinLength = state.pin.length,
                    isError = hasError,
                    metrics = metrics
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TiplyNumericKeypad(
                onDigit = { digit ->
                    if (!state.isLoading && state.pin.length < PIN_LENGTH) {
                        onDigit(digit)
                    }
                },
                onDelete = {
                    if (!state.isLoading) {
                        onDelete()
                    }
                },
                onConfirm = {},
                confirmEnabled = false,
                isLoading = state.isLoading,
                touchSize = metrics.keypadTouchSize,
                digitFontSize = metrics.keypadDigitSize.sp,
                iconSize = metrics.keypadIconSize,
                digitColor = PrimaryTextColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = metrics.keypadBottomPadding)
            )
        }
    }
}

@Composable
private fun PinDots(
    pinLength: Int,
    isError: Boolean,
    metrics: LoginLayoutMetrics
) {
    Row(horizontalArrangement = Arrangement.spacedBy(metrics.dotSpacing)) {
        repeat(PIN_LENGTH) { index ->
            val isFilled = index < pinLength

            val fillBrush = when {
                isError -> Brush.radialGradient(
                    listOf(
                        Color(0xFFFF9AA0),
                        Color(0xFFE44751)
                    )
                )

                isFilled -> Brush.radialGradient(
                    listOf(
                        Color(0xFF7AF3EC),
                        Color(0xFF0ABFD8)
                    )
                )

                else -> Brush.radialGradient(
                    listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFE1E4E8)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(metrics.dotSize)
                    .shadow(
                        elevation = if (isFilled || isError) {
                            metrics.dotShadowFilled
                        } else {
                            metrics.dotShadowEmpty
                        },
                        shape = CircleShape,
                        ambientColor = when {
                            isError -> Color(0x55FF7A81)
                            isFilled -> Color(0x4435DAD8)
                            else -> Color.Black.copy(alpha = 0.10f)
                        },
                        spotColor = when {
                            isError -> Color(0x66FF7A81)
                            isFilled -> Color(0x5520E3DE)
                            else -> Color.Black.copy(alpha = 0.08f)
                        }
                    )
                    .background(
                        color = Color(0xFFF9FAFB),
                        shape = CircleShape
                    )
                    .padding(metrics.dotPadding),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fillBrush, CircleShape)
                )
            }
        }
    }
}

private const val PIN_LENGTH = 4
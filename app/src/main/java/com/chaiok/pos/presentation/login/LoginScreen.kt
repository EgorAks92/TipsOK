package com.chaiok.pos.presentation.login

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private val LightScreenColor = Color(0xFFFFFFFF)
private val PrimaryTextColor = Color(0xFF1B2128)
private val SecondaryTextColor = Color(0xFF69707A)
private val StrokeColor = Color(0xFFE2E7EF)
private val AccentColor = Color(0xFF087BE8)
private val GreenColor = Color(0xFF14B8A6)

private data class LoginLayoutMetrics(
    val isSquareCompact: Boolean,

    val horizontalPadding: Dp,
    val verticalPadding: Dp,

    val closeTouchSize: Dp,
    val closeIconSize: Dp,
    val closeBottomSpacer: Dp,

    val logoWidth: Dp,
    val logoHeight: Dp,
    val logoTitleSpacer: Dp,

    val pinPanelHeight: Dp,
    val pinPanelCornerRadius: Dp,
    val pinPanelShadowElevation: Dp,
    val pinPanelHorizontalPadding: Dp,

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
    val keypadDigitSize: Int,
    val keypadRowSpacing: Dp?
)

private fun squarePremiumLoginMetrics(): LoginLayoutMetrics {
    return LoginLayoutMetrics(
        isSquareCompact = true,

        horizontalPadding = 18.dp,
        verticalPadding = 10.dp,

        closeTouchSize = 40.dp,
        closeIconSize = 24.dp,
        closeBottomSpacer = 6.dp,

        logoWidth = 88.dp,
        logoHeight = 32.dp,
        logoTitleSpacer = 12.dp,

        pinPanelHeight = 118.dp,
        pinPanelCornerRadius = 26.dp,
        pinPanelShadowElevation = 5.dp,
        pinPanelHorizontalPadding = 16.dp,

        titleSize = 16,
        titleLineHeight = 20,
        titleDotsSpacer = 14.dp,

        dotSize = 34.dp,
        dotPadding = 5.dp,
        dotSpacing = 14.dp,
        dotShadowFilled = 8.dp,
        dotShadowEmpty = 5.dp,

        keypadBottomPadding = 8.dp,
        keypadTouchSize = 56.dp,
        keypadIconSize = 28.dp,
        keypadDigitSize = 24,
        keypadRowSpacing = 0.dp
    )
}

private fun regularLoginMetrics(
    isCompactPortrait: Boolean
): LoginLayoutMetrics {
    return if (isCompactPortrait) {
        LoginLayoutMetrics(
            isSquareCompact = false,

            horizontalPadding = 24.dp,
            verticalPadding = 14.dp,

            closeTouchSize = 48.dp,
            closeIconSize = 34.dp,
            closeBottomSpacer = 28.dp,

            logoWidth = 120.dp,
            logoHeight = 54.dp,
            logoTitleSpacer = 116.dp,

            pinPanelHeight = 0.dp,
            pinPanelCornerRadius = 0.dp,
            pinPanelShadowElevation = 0.dp,
            pinPanelHorizontalPadding = 0.dp,

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
            keypadDigitSize = 30,
            keypadRowSpacing = null
        )
    } else {
        LoginLayoutMetrics(
            isSquareCompact = false,

            horizontalPadding = 24.dp,
            verticalPadding = 14.dp,

            closeTouchSize = 48.dp,
            closeIconSize = 34.dp,
            closeBottomSpacer = 28.dp,

            logoWidth = 120.dp,
            logoHeight = 54.dp,
            logoTitleSpacer = 116.dp,

            pinPanelHeight = 0.dp,
            pinPanelCornerRadius = 0.dp,
            pinPanelShadowElevation = 0.dp,
            pinPanelHorizontalPadding = 0.dp,

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
            keypadDigitSize = 32,
            keypadRowSpacing = null
        )
    }
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onLogin: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val deviceClass = rememberChaiOkDeviceClass()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isCompactPortrait = screenHeight < 780.dp

    val metrics = when (deviceClass) {
        ChaiOkDeviceClass.SquareCompact -> squarePremiumLoginMetrics()
        ChaiOkDeviceClass.Regular -> regularLoginMetrics(isCompactPortrait = isCompactPortrait)
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

    when (metrics.isSquareCompact) {
        true -> {
            LoginSquarePremiumContent(
                state = state,
                metrics = metrics,
                hasError = hasError,
                shakeValue = shake.value,
                onDigit = onDigit,
                onDelete = onDelete,
                onClose = {
                    onClose?.invoke() ?: (context as? Activity)?.finish()
                }
            )
        }

        false -> {
            LoginRegularContent(
                state = state,
                metrics = metrics,
                hasError = hasError,
                shakeValue = shake.value,
                onDigit = onDigit,
                onDelete = onDelete,
                onClose = {
                    onClose?.invoke() ?: (context as? Activity)?.finish()
                }
            )
        }
    }
}

@Composable
private fun LoginRegularContent(
    state: LoginUiState,
    metrics: LoginLayoutMetrics,
    hasError: Boolean,
    shakeValue: Float,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
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
            CloseRow(
                metrics = metrics,
                onClose = onClose
            )

            Spacer(modifier = Modifier.height(metrics.closeBottomSpacer))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeValue },
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

                LoginTitle(metrics = metrics)

                Spacer(modifier = Modifier.height(metrics.titleDotsSpacer))

                PinDots(
                    pinLength = state.pin.length,
                    isError = hasError,
                    metrics = metrics
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            LoginKeypad(
                state = state,
                metrics = metrics,
                onDigit = onDigit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun LoginSquarePremiumContent(
    state: LoginUiState,
    metrics: LoginLayoutMetrics,
    hasError: Boolean,
    shakeValue: Float,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
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
            CloseRow(
                metrics = metrics,
                onClose = onClose
            )

            Spacer(modifier = Modifier.height(metrics.closeBottomSpacer))

            Image(
                painter = painterResource(id = R.drawable.tiply_logo_black),
                contentDescription = "Tiply",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(
                        width = metrics.logoWidth,
                        height = metrics.logoHeight
                    ),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(metrics.logoTitleSpacer))

            PinPanel(
                pinLength = state.pin.length,
                isError = hasError,
                shakeValue = shakeValue,
                metrics = metrics
            )

            Spacer(modifier = Modifier.weight(1f))

            LoginKeypad(
                state = state,
                metrics = metrics,
                onDigit = onDigit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun CloseRow(
    metrics: LoginLayoutMetrics,
    onClose: () -> Unit
) {
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
                    onClick = onClose
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
}

@Composable
private fun PinPanel(
    pinLength: Int,
    isError: Boolean,
    shakeValue: Float,
    metrics: LoginLayoutMetrics
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.pinPanelHeight)
            .graphicsLayer { translationX = shakeValue }
            .shadow(
                elevation = metrics.pinPanelShadowElevation,
                shape = RoundedCornerShape(metrics.pinPanelCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.055f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(metrics.pinPanelCornerRadius))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isError) {
                    Color(0xFFFF5A5F).copy(alpha = 0.28f)
                } else {
                    StrokeColor.copy(alpha = 0.86f)
                },
                shape = RoundedCornerShape(metrics.pinPanelCornerRadius)
            )
            .padding(horizontal = metrics.pinPanelHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Введите PIN-код",
            color = PrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontSize = metrics.titleSize.sp,
            lineHeight = metrics.titleLineHeight.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(metrics.titleDotsSpacer))

        PinDots(
            pinLength = pinLength,
            isError = isError,
            metrics = metrics
        )

        if (isError) {
            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = "Неверный PIN",
                color = Color(0xFFE44751),
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LoginTitle(
    metrics: LoginLayoutMetrics
) {
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
}

@Composable
private fun LoginKeypad(
    state: LoginUiState,
    metrics: LoginLayoutMetrics,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit
) {
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
        rowSpacing = metrics.keypadRowSpacing,
        iconSize = metrics.keypadIconSize,
        digitColor = PrimaryTextColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = metrics.keypadBottomPadding)
    )
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
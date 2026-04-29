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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily


private val LightScreenColor = Color(0xFFFFFFFF)
private val PrimaryTextColor = Color(0xFF1B2128)

@Composable
fun LoginScreen(
    state: LoginUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onLogin: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val hasError = state.errorMessage != null
    val checkEnabled = state.pin.length == 4 && !state.isLoading

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
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val closeInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = { onClose?.invoke() ?: (context as? Activity)?.finish() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Закрыть",
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit,
                        alpha = 0.95f
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tiply_logo_black),
                    contentDescription = "Tiply",
                    modifier = Modifier.size(width = 120.dp, height = 54.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(116.dp))

                Text(
                    text = "Введите PIN-код",
                    color = PrimaryTextColor,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MontserratFontFamily,
                        fontSize = 16.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                PinDots(
                    pinLength = state.pin.length,
                    isError = hasError
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TiplyNumericKeypad(
                onDigit = onDigit,
                onDelete = onDelete,
                onConfirm = onLogin,
                confirmEnabled = checkEnabled,
                isLoading = state.isLoading,
                digitColor = PrimaryTextColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun PinDots(pinLength: Int, isError: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(4) { index ->
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
                    .size(46.dp)
                    .shadow(
                        elevation = if (isFilled || isError) 12.dp else 9.dp,
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
                    .padding(6.dp),
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

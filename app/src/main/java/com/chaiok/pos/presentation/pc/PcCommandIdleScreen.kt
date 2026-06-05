package com.chaiok.pos.presentation.pc

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chaiok.pos.R
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.components.TiplyNumericKeypad
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PcCommandIdleScreen(
    state: PcCommandIdleUiState,
    onRequestUnlock: () -> Unit,
    onCancelUnlock: () -> Unit,
    onUnlockDigit: (String) -> Unit,
    onUnlockBackspace: () -> Unit,
    onSubmitUnlockPin: () -> Unit,
    onFeedbackServiceRatingSelected: (Int) -> Unit,
    onFeedbackKitchenRatingSelected: (Int) -> Unit,
    onFeedbackClose: () -> Unit,
    onFeedbackTimerTick: () -> Unit
) {
    val slides = if (state.images.isEmpty()) listOf(DEFAULT_IMAGE) else state.images
    val isCompact = rememberChaiOkDeviceClass() == ChaiOkDeviceClass.SquareCompact
    var currentIndex by remember(slides) { mutableIntStateOf(0) }

    LaunchedEffect(slides) {
        currentIndex = 0
        if (slides.size > 1) {
            while (true) {
                delay(SLIDE_INTERVAL_MS)
                currentIndex = (currentIndex + 1) % slides.size
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        PremiumIdleSlideshow(
            slides = slides,
            currentIndex = currentIndex,
            modifier = Modifier.fillMaxSize()
        )

        StatusChip(
            status = state.connectionStatus,
            message = state.statusMessage,
            onClick = onRequestUnlock,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (isCompact) 10.dp else 18.dp)
        )

        if (state.postPaymentFeedback.visible) {
            LaunchedEffect(
                state.postPaymentFeedback.visible,
                state.postPaymentFeedback.commandId
            ) {
                while (state.postPaymentFeedback.visible && state.postPaymentFeedback.secondsLeft > 0) {
                    delay(1_000L)
                    onFeedbackTimerTick()
                }
            }

            PcPostPaymentFeedbackScreen(
                designStyle = state.designStyle,
                serviceRating = state.postPaymentFeedback.serviceRating,
                kitchenRating = state.postPaymentFeedback.kitchenRating,
                secondsLeft = state.postPaymentFeedback.secondsLeft,
                onServiceRatingSelected = onFeedbackServiceRatingSelected,
                onKitchenRatingSelected = onFeedbackKitchenRatingSelected,
                onClose = onFeedbackClose,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (state.showUnlockDialog) {
            UnlockPinDialog(
                pin = state.unlockPin,
                maxPinLength = state.unlockPinMaxLength,
                error = state.unlockError,
                isLoading = state.isUnlocking,
                isCompact = isCompact,
                onCancel = onCancelUnlock,
                onDigit = onUnlockDigit,
                onBackspace = onUnlockBackspace,
                onSubmit = onSubmitUnlockPin
            )
        }
    }
}

@Composable
fun PcPostPaymentFeedbackScreen(
    designStyle: PcCompactPaymentDesignStyle,
    serviceRating: Int?,
    kitchenRating: Int?,
    secondsLeft: Int,
    onServiceRatingSelected: (Int) -> Unit,
    onKitchenRatingSelected: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAlfa = designStyle == PcCompactPaymentDesignStyle.ALFA
    val isCompact = rememberChaiOkDeviceClass() == ChaiOkDeviceClass.SquareCompact
    val titleColor = if (isAlfa) Color(0xFF181F28) else Color.White
    val selectedStar = if (isAlfa) Color(0xFFE30613) else Color(0xFF42E4E8)
    val unselectedStar = if (isAlfa) Color(0xFFD7E9FF) else Color(0xFF35546D)
    val outlineStar = if (isAlfa) Color(0xFFEAF4FF) else Color(0xFF90C7DD)
    val timerColor = if (isAlfa) Color(0xFF181F28) else Color.White
    val titleSize = if (isCompact) 35.sp else 64.sp
    val starSize = if (isCompact) 48.dp else 92.dp

    val closeIconRes = if (isAlfa) {
        R.drawable.ic_alfa_payment_close
    } else {
        R.drawable.ic_payment_close
    }

    Box(
        modifier = modifier.background(
            if (isAlfa) {
                Brush.verticalGradient(listOf(Color.White, Color.White))
            } else {
                Brush.linearGradient(listOf(Color(0xFF0F1720), Color(0xFF1269A8), Color(0xFF111A23)))
            }
        )
    ) {
        IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (isCompact) 18.dp else 48.dp)
                    .size(if (isCompact) 56.dp else 88.dp)
            ) {
            Image(
                painter = painterResource(id = closeIconRes),
                contentDescription = "Закрыть",
                modifier = Modifier.size(if (isCompact) 28.dp else 48.dp)
            )
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isCompact) 24.dp else 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FeedbackQuestion(
                text = "Как вам сервис?",
                titleColor = titleColor,
                titleSize = titleSize,
                starSize = starSize,
                selectedStar = selectedStar,
                unselectedStar = unselectedStar,
                outlineStar = outlineStar,
                selectedRating = serviceRating,
                glow = !isAlfa,
                onRatingSelected = onServiceRatingSelected
            )
            Spacer(Modifier.fillMaxHeight(if (isCompact) 0.12f else 0.16f))
            FeedbackQuestion(
                text = "Как вам кухня?",
                titleColor = titleColor,
                titleSize = titleSize,
                starSize = starSize,
                selectedStar = selectedStar,
                unselectedStar = unselectedStar,
                outlineStar = outlineStar,
                selectedRating = kitchenRating,
                glow = !isAlfa,
                onRatingSelected = onKitchenRatingSelected
            )
            Spacer(Modifier.fillMaxHeight(if (isCompact) 0.08f else 0.10f))
        }

        FeedbackTimer(
            secondsLeft = secondsLeft,
            color = timerColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(if (isCompact) 16.dp else 56.dp)
                .size(if (isCompact) 52.dp else 112.dp)
        )
    }
}

@Composable
private fun FeedbackQuestion(
    text: String,
    titleColor: Color,
    titleSize: androidx.compose.ui.unit.TextUnit,
    starSize: Dp,
    selectedStar: Color,
    unselectedStar: Color,
    outlineStar: Color,
    selectedRating: Int?,
    glow: Boolean,
    onRatingSelected: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            color = titleColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = titleSize,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.padding(top = if (starSize < 60.dp) 18.dp else 56.dp),
            horizontalArrangement = Arrangement.spacedBy(if (starSize < 60.dp) 20.dp else 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val value = index + 1
                FeedbackRatingStarButton(
                    filled = selectedRating?.let { value <= it } == true,
                    selectedColor = selectedStar,
                    unselectedColor = unselectedStar,
                    outlineColor = outlineStar,
                    useGlass = glow,
                    starIconSize = if (glow) {
                        if (starSize < 60.dp) 28.dp else 54.dp
                    } else {
                        starSize
                    },
                    cornerRadius = if (starSize < 60.dp) 16.dp else 28.dp,
                    onClick = { onRatingSelected(value) },
                    modifier = Modifier.size(starSize)
                )
            }
        }
    }
}


private fun Modifier.feedbackStarBlurBackdrop(
    enabled: Boolean,
    alpha: Float,
    shapeRadius: Dp
): Modifier = if (!enabled || alpha <= 0f) {
    this
} else {
    this.drawBehind {
        val radiusPx = shapeRadius.toPx()
        val blurRadiusPx = 14.dp.toPx()

        val frameworkPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = (255 * alpha.coerceIn(0f, 1f)).toInt()
            shader = android.graphics.LinearGradient(
                0f,
                0f,
                0f,
                size.height,
                intArrayOf(
                    android.graphics.Color.rgb(0x8A, 0xFF, 0xF7),
                    android.graphics.Color.rgb(0x20, 0xD6, 0xD2),
                    android.graphics.Color.rgb(0x11, 0x8B, 0xD7)
                ),
                floatArrayOf(0f, 0.52f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            maskFilter = android.graphics.BlurMaskFilter(
                blurRadiusPx,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                radiusPx,
                radiusPx,
                frameworkPaint
            )
        }
    }
}

@Composable
private fun FeedbackRatingStarButton(
    filled: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    outlineColor: Color,
    useGlass: Boolean,
    starIconSize: Dp,
    cornerRadius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)

    val glassBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.20f),
            Color(0xFFE7FFFF).copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.07f)
        )
    )

    val borderColor = when {
        filled && useGlass -> Color.White.copy(alpha = 0.46f)
        useGlass -> Color.White.copy(alpha = 0.22f)
        filled -> selectedColor.copy(alpha = 0.55f)
        else -> outlineColor
    }

    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        if (useGlass && filled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .feedbackStarBlurBackdrop(
                        enabled = true,
                        alpha = 0.62f,
                        shapeRadius = cornerRadius
                    )
            )
        }

        if (useGlass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(glassBrush)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = shape
                    )
            )
        }

        RatingStar(
            filled = filled,
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            outlineColor = outlineColor,
            glow = false,
            modifier = Modifier.size(starIconSize)
        )
    }
}

@Composable
private fun RatingStar(
    filled: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    outlineColor: Color,
    glow: Boolean,
    modifier: Modifier = Modifier
) {
    val fill = if (filled) selectedColor else unselectedColor
    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) / 2f * 0.92f
        val innerRadius = radius * 0.52f
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        for (i in 0 until 10) {
            val angle = -PI / 2.0 + i * PI / 5.0
            val pointRadius = if (i % 2 == 0) radius else innerRadius
            val x = center.x + (cos(angle) * pointRadius).toFloat()
            val y = center.y + (sin(angle) * pointRadius).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, fill)
        drawPath(
            path = path,
            color = if (filled) selectedColor.copy(alpha = 0.55f) else outlineColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun FeedbackTimer(
    secondsLeft: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.border(width = 1.5.dp, color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = secondsLeft.toString(),
            color = color,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = if (secondsLeft >= 10) 36.sp else 28.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UnlockPinDialog(
    pin: String,
    maxPinLength: Int,
    error: String?,
    isLoading: Boolean,
    isCompact: Boolean,
    onCancel: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val dialogPaddingHorizontal = if (isCompact) 16.dp else 20.dp
    val dialogPaddingVertical = if (isCompact) 14.dp else 18.dp
    val contentSpacing = if (isCompact) 10.dp else 14.dp
    val keypadTouchSize = if (isCompact) 50.dp else 58.dp
    val keypadIconSize = if (isCompact) 24.dp else 28.dp
    val keypadFontSize = if (isCompact) 22.sp else 24.sp
    val titleFontSize = if (isCompact) 17.sp else 18.sp

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dialogPaddingHorizontal,
                        vertical = dialogPaddingVertical
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(contentSpacing)
            ) {
                Text(
                    text = "Введите пароль",
                    color = Color(0xFF1B2128),
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = titleFontSize,
                    textAlign = TextAlign.Center
                )

                PinDots(
                    pinLength = pin.length,
                    maxLength = maxPinLength
                )

                Text(
                    text = error.orEmpty(),
                    color = if (error != null) Color(0xFFD32F2F) else Color.Transparent,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    minLines = 1
                )

                TiplyNumericKeypad(
                    onDigit = onDigit,
                    onDelete = onBackspace,
                    onConfirm = onSubmit,
                    confirmEnabled = pin.isNotBlank() && !isLoading,
                    isLoading = isLoading,
                    digitColor = Color(0xFF1B2128),
                    touchSize = keypadTouchSize,
                    iconSize = keypadIconSize,
                    digitFontSize = keypadFontSize,
                    rowSpacing = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onCancel,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F6FB),
                        contentColor = Color(0xFF1B2128)
                    )
                ) {
                    Text(
                        text = "Отмена",
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PinDots(pinLength: Int, maxLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(maxLength) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (index < pinLength) {
                            Color(0xFF1B2128)
                        } else {
                            Color(0xFFD5DBE5)
                        },
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun PremiumIdleSlideshow(
    slides: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = slides[currentIndex],
        animationSpec = tween(durationMillis = 1_350, easing = FastOutSlowInEasing),
        modifier = modifier,
        label = "pc_idle_premium_dissolve"
    ) { image ->
        IdleBackgroundImage(
            image = image,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StatusChip(
    status: PcUsbConnectionStatus,
    message: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = message ?: when (status) {
        PcUsbConnectionStatus.Idle -> "USB готов"
        PcUsbConnectionStatus.WaitingForData -> "Ожидание команды"
        is PcUsbConnectionStatus.Error -> "Ошибка USB"
        else -> "Подключение"
    }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(14.dp),
        color = if (status is PcUsbConnectionStatus.Error) {
            Color(0x99D32F2F)
        } else {
            Color(0x77373D45)
        }
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun IdleBackgroundImage(image: String, modifier: Modifier) {
    val isUri = image.startsWith("content://", ignoreCase = true) ||
            image.startsWith("file://", ignoreCase = true)

    if (!isUri || image.isBlank() || image == DEFAULT_IMAGE) {
        FallbackBackground(modifier)
        return
    }

    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, image) {
        value = loadImageBitmapFromUri(context, image)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        FallbackBackground(modifier)
    }
}

@Composable
private fun FallbackBackground(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1D4ED8),
                    Color(0xFF0EA5E9)
                )
            )
        )
    ) {
        Image(
            painter = painterResource(id = R.drawable.waiter_card_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.20f
        )
    }
}

private suspend fun loadImageBitmapFromUri(
    context: Context,
    uriString: String
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(uriString)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        bitmap.asImageBitmap()
    }.getOrNull()
}

private const val DEFAULT_IMAGE = "default"
private const val SLIDE_INTERVAL_MS = 6_000L
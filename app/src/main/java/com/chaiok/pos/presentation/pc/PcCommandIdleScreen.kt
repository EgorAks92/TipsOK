package com.chaiok.pos.presentation.pc

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.domain.model.PcUsbConnectionStatus
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun PcCommandIdleScreen(state: PcCommandIdleUiState, onOpenSettings: () -> Unit) {
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
        PremiumIdleSlideshow(slides = slides, currentIndex = currentIndex, modifier = Modifier.fillMaxSize())

        StatusChip(
            status = state.connectionStatus,
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (isCompact) 10.dp else 18.dp)
        )
    }
}

@Composable
private fun PremiumIdleSlideshow(slides: List<String>, currentIndex: Int, modifier: Modifier = Modifier) {
    val motionProgress = remember(currentIndex) { Animatable(0f) }
    LaunchedEffect(currentIndex) {
        motionProgress.snapTo(0f)
        motionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = SLIDE_INTERVAL_MS.toInt(), easing = LinearEasing)
        )
    }

    val scale = 1f + (0.045f * motionProgress.value)
    val panMagnitude = 16f * motionProgress.value
    val isEvenSlide = currentIndex % 2 == 0
    val panX = if (isEvenSlide) -panMagnitude else panMagnitude
    val panY = if (isEvenSlide) -panMagnitude * 0.6f else panMagnitude * 0.6f

    Crossfade(
        targetState = slides[currentIndex],
        animationSpec = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
        modifier = modifier,
        label = "pc_idle_premium_crossfade"
    ) { image ->
        IdleBackgroundImage(
            image = image,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = panX
                    translationY = panY
                }
        )
    }
}

@Composable
private fun StatusChip(status: PcUsbConnectionStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val text = when (status) {
        PcUsbConnectionStatus.Idle -> "USB готов"
        PcUsbConnectionStatus.WaitingForData -> "Ожидание команды"
        is PcUsbConnectionStatus.Error -> "Ошибка USB"
        else -> "Подключение"
    }
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (status is PcUsbConnectionStatus.Error) Color(0x99D32F2F) else Color(0x77373D45)
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
    val isUri = image.startsWith("content://", true) || image.startsWith("file://", true)
    if (!isUri || image.isBlank() || image == DEFAULT_IMAGE) {
        FallbackBackground(modifier)
        return
    }

    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, image) { value = loadImageBitmapFromUri(context, image) }
    if (bitmap != null) {
        Image(bitmap = bitmap!!, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        FallbackBackground(modifier)
    }
}

@Composable
private fun FallbackBackground(modifier: Modifier) {
    Box(modifier = modifier.background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1D4ED8), Color(0xFF0EA5E9))))) {
        Image(
            painter = painterResource(id = R.drawable.waiter_card_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.20f
        )
    }
}

private suspend fun loadImageBitmapFromUri(context: Context, uriString: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(uriString)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        bitmap.asImageBitmap()
    }.getOrNull()
}

private const val DEFAULT_IMAGE = "default"
private const val SLIDE_INTERVAL_MS = 6_000L

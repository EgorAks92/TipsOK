package com.chaiok.pos.presentation.pc

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        Crossfade(targetState = slides[currentIndex], label = "pc_idle_crossfade") {
            IdleBackgroundImage(image = it, modifier = Modifier.fillMaxSize())
        }

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.33f)))

        Column(Modifier.fillMaxSize().padding(if (isCompact) 12.dp else 24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("ChaiOK", color = Color.White, fontFamily = MontserratFontFamily, fontWeight = FontWeight.Bold, fontSize = if (isCompact) 18.sp else 24.sp)
                    Text("by Tiply", color = Color.White.copy(alpha = 0.82f), fontFamily = MontserratFontFamily, fontSize = if (isCompact) 10.sp else 12.sp)
                }
                StatusChip(state.connectionStatus)
            }

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.title, color = Color.White, fontFamily = MontserratFontFamily, fontWeight = FontWeight.Bold, fontSize = if (isCompact) 26.sp else 36.sp)
                    Text(state.subtitle, color = Color.White.copy(alpha = 0.96f), fontFamily = MontserratFontFamily, fontWeight = FontWeight.SemiBold, fontSize = if (isCompact) 15.sp else 20.sp, modifier = Modifier.padding(top = 10.dp))
                    Text(state.helperText, color = Color.White.copy(alpha = 0.88f), fontFamily = MontserratFontFamily, fontSize = if (isCompact) 12.sp else 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }

            TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) {
                Text("Настройки", color = Color.White, fontFamily = MontserratFontFamily)
            }
        }
    }
}

@Composable
private fun StatusChip(status: PcUsbConnectionStatus) {
    val text = when (status) {
        PcUsbConnectionStatus.Idle -> "USB готов"
        PcUsbConnectionStatus.WaitingForData -> "Ожидание команды"
        is PcUsbConnectionStatus.Error -> "Ошибка USB"
        else -> "Подключение"
    }
    Surface(shape = RoundedCornerShape(16.dp), color = if (status is PcUsbConnectionStatus.Error) Color(0x99D32F2F) else Color(0x664CAF50)) {
        Text(text, color = Color.White, fontFamily = MontserratFontFamily, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
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

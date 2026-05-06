package com.chaiok.pos.presentation.pc

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.Image

@Composable
fun PcCommandIdleScreen(
    state: PcCommandIdleUiState,
    onOpenSettings: () -> Unit
) {
    val images = if (state.images.isEmpty()) listOf(DEFAULT_IMAGE) else state.images
    var currentIndex by remember(images) { mutableIntStateOf(0) }

    LaunchedEffect(images) {
        currentIndex = 0
        if (images.size > 1) {
            while (true) {
                delay(SLIDE_DELAY_MS)
                currentIndex = (currentIndex + 1) % images.size
            }
        }
    }

    val compact = rememberChaiOkDeviceClass() == ChaiOkDeviceClass.SquareCompact

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = images[currentIndex]) { image ->
            IdleBackgroundImage(
                image = image,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 12.dp else 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "ChaiOK",
                        color = Color.White,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 18.sp else 24.sp
                    )
                    Text(
                        text = "by Tiply",
                        color = Color.White.copy(alpha = 0.82f),
                        fontFamily = MontserratFontFamily,
                        fontSize = if (compact) 10.sp else 12.sp
                    )
                }

                StatusChip(state.connectionStatus)
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.title,
                        color = Color.White,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 28.sp else 36.sp
                    )
                    Text(
                        text = state.subtitle,
                        color = Color.White.copy(alpha = 0.95f),
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 16.sp else 20.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        text = state.helperText,
                        color = Color.White.copy(alpha = 0.88f),
                        fontFamily = MontserratFontFamily,
                        fontSize = if (compact) 12.sp else 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
    val isError = status is PcUsbConnectionStatus.Error
    Surface(
        color = if (isError) Color(0x88D32F2F) else Color(0x664CAF50),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status.toDisplayText(),
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun IdleBackgroundImage(image: String, modifier: Modifier = Modifier) {
    if (image.isCustomBackgroundUri()) {
        val context = LocalContext.current
        val bitmapState = produceState<ImageBitmap?>(null, image) {
            value = loadImageBitmapFromUri(context, image)
        }

        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
        } else {
            DefaultImage(modifier)
        }
    } else {
        DefaultImage(modifier)
    }
}

@Composable
private fun DefaultImage(modifier: Modifier) {
    Image(
        painter = painterResource(id = R.drawable.waiter_card_background),
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(0.dp)),
        contentScale = ContentScale.Crop
    )
}

private fun String.isCustomBackgroundUri(): Boolean {
    return startsWith("content://", true) || startsWith("file://", true)
}

private suspend fun loadImageBitmapFromUri(context: Context, uriString: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
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
}

private fun PcUsbConnectionStatus.toDisplayText(): String {
    return when (this) {
        PcUsbConnectionStatus.Idle -> "USB готов"
        PcUsbConnectionStatus.BindingService -> "Подключение к сервису"
        PcUsbConnectionStatus.ServiceBound -> "Сервис подключён"
        PcUsbConnectionStatus.OpeningPort -> "Открытие порта"
        PcUsbConnectionStatus.ConnectingPort -> "Подключение порта"
        PcUsbConnectionStatus.Connected -> "Порт подключён"
        PcUsbConnectionStatus.WaitingForData -> "Ожидание команды"
        is PcUsbConnectionStatus.Error -> "Ошибка USB"
    }
}

private const val DEFAULT_IMAGE = "default"
private const val SLIDE_DELAY_MS = 5_000L

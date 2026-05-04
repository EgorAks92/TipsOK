package com.chaiok.pos.presentation.background

import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BackgroundScreenColor = Color.White
private val BackgroundPrimaryTextColor = Color(0xFF1B2128)
private val BackgroundSecondaryTextColor = Color(0xFF69707A)
private val BackgroundCardColor = Color(0xFFF7F8FA)
private val BackgroundAccentColor = Color(0xFF087BE8)
private val BackgroundGreenColor = Color(0xFF14B8A6)

@Composable
fun ProfileBackgroundScreen(
    state: ProfileBackgroundUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedBackground = state.selectedBackground
    val hasCustomImage = selectedBackground.isCustomBackgroundUri()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            onSelect(uri.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundScreenColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TiplyBackTopAppBar(
                title = "Фон профиля",
                onBack = onBack,
                elevation = 14.dp,
                ambientAlpha = 0.64f,
                spotAlpha = 0.72f
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Фон официанта",
                    color = BackgroundPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 28.sp
                )

                Text(
                    text = "Выберите изображение из файлового менеджера. Оно будет использоваться как фон карточки официанта.",
                    color = BackgroundSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                BackgroundPreviewCard(
                    selectedBackground = selectedBackground,
                    hasCustomImage = hasCustomImage
                )

                Spacer(modifier = Modifier.height(8.dp))

                PickImageButton(
                    text = if (hasCustomImage) {
                        "Выбрать другое изображение"
                    } else {
                        "Выбрать изображение"
                    },
                    onClick = {
                        imagePicker.launch(arrayOf("image/*"))
                    }
                )

                if (hasCustomImage) {
                    ResetBackgroundButton(
                        onClick = { onSelect(DEFAULT_BACKGROUND) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundPreviewCard(
    selectedBackground: String?,
    hasCustomImage: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(BackgroundCardColor)
            .padding(16.dp)
    ) {
        Text(
            text = when {
                selectedBackground == null -> "Загрузка фона"
                hasCustomImage -> "Выбранное изображение"
                else -> "Стандартный фон"
            },
            color = BackgroundPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.28f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFF0F2F4)),
            contentAlignment = Alignment.Center
        ) {
            when {
                selectedBackground == null -> {
                    LoadingBackgroundPreview(
                        text = "Загрузка фона..."
                    )
                }

                hasCustomImage -> {
                    UriPreviewImage(
                        uriString = selectedBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.waiter_card_background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when {
                selectedBackground == null -> {
                    "Получаем сохранённые настройки фона."
                }

                hasCustomImage -> {
                    "Изображение сохранено и будет использоваться как фон плашки."
                }

                else -> {
                    "Сейчас используется стандартный фон плашки официанта."
                }
            },
            color = BackgroundSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun UriPreviewImage(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val previewState = produceState<PreviewImageState>(
        initialValue = PreviewImageState.Loading,
        key1 = uriString
    ) {
        val bitmap = loadImageBitmapFromUri(
            context = context,
            uriString = uriString
        )

        value = if (bitmap != null) {
            PreviewImageState.Loaded(bitmap)
        } else {
            PreviewImageState.Failed
        }
    }

    when (val state = previewState.value) {
        PreviewImageState.Loading -> {
            LoadingBackgroundPreview(
                text = "Загрузка изображения...",
                modifier = modifier
            )
        }

        PreviewImageState.Failed -> {
            LoadingBackgroundPreview(
                text = "Не удалось загрузить изображение",
                modifier = modifier
            )
        }

        is PreviewImageState.Loaded -> {
            Image(
                bitmap = state.bitmap,
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun LoadingBackgroundPreview(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFFEFF2F5),
                    Color(0xFFF8F9FA)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = BackgroundSecondaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PickImageButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        BackgroundAccentColor,
                        BackgroundGreenColor
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResetBackgroundButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF7F8FA))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Вернуть стандартный фон",
            color = BackgroundPrimaryTextColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}

private sealed interface PreviewImageState {
    data object Loading : PreviewImageState
    data object Failed : PreviewImageState
    data class Loaded(val bitmap: ImageBitmap) : PreviewImageState
}

private fun String?.isCustomBackgroundUri(): Boolean {
    val value = this ?: return false

    return value.startsWith("content://", ignoreCase = true) ||
            value.startsWith("file://", ignoreCase = true)
}

private suspend fun loadImageBitmapFromUri(
    context: Context,
    uriString: String
): ImageBitmap? {
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

private const val DEFAULT_BACKGROUND = "default"
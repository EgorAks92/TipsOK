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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
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
private val BackgroundStrokeColor = Color(0xFFE2E7EF)

private data class ProfileBackgroundLayoutMetrics(
    val contentHorizontalPadding: Dp,
    val contentTopPadding: Dp,
    val contentBottomPadding: Dp,
    val contentSpacing: Dp,

    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val subtitleFontSize: TextUnit,
    val subtitleLineHeight: TextUnit,
    val subtitleMaxLines: Int,

    val previewCardCornerRadius: Dp,
    val previewCardShadowElevation: Dp,
    val previewCardPadding: Dp,
    val previewTitleFontSize: TextUnit,
    val previewTitleLineHeight: TextUnit,
    val previewTitleMaxLines: Int,
    val previewTitleToImageSpacing: Dp,

    val previewImageHeight: Dp,
    val previewImageCornerRadius: Dp,
    val previewImageShadowElevation: Dp,

    val previewImageToDescriptionSpacing: Dp,
    val previewDescriptionFontSize: TextUnit,
    val previewDescriptionLineHeight: TextUnit,
    val previewDescriptionMaxLines: Int,

    val loadingTextFontSize: TextUnit,
    val loadingTextLineHeight: TextUnit,

    val primaryButtonHeight: Dp,
    val primaryButtonCornerRadius: Dp,
    val primaryButtonShadowElevation: Dp,
    val primaryButtonFontSize: TextUnit,
    val primaryButtonLineHeight: TextUnit,

    val resetButtonHeight: Dp,
    val resetButtonCornerRadius: Dp,
    val resetButtonShadowElevation: Dp,
    val resetButtonFontSize: TextUnit,
    val resetButtonLineHeight: TextUnit
)

private fun regularProfileBackgroundMetrics(): ProfileBackgroundLayoutMetrics {
    return ProfileBackgroundLayoutMetrics(
        contentHorizontalPadding = 24.dp,
        contentTopPadding = 28.dp,
        contentBottomPadding = 24.dp,
        contentSpacing = 14.dp,

        titleFontSize = 24.sp,
        titleLineHeight = 28.sp,
        subtitleFontSize = 14.sp,
        subtitleLineHeight = 19.sp,
        subtitleMaxLines = Int.MAX_VALUE,

        previewCardCornerRadius = 28.dp,
        previewCardShadowElevation = 8.dp,
        previewCardPadding = 16.dp,
        previewTitleFontSize = 16.sp,
        previewTitleLineHeight = 20.sp,
        previewTitleMaxLines = 1,
        previewTitleToImageSpacing = 12.dp,

        previewImageHeight = 178.dp,
        previewImageCornerRadius = 32.dp,
        previewImageShadowElevation = 10.dp,

        previewImageToDescriptionSpacing = 12.dp,
        previewDescriptionFontSize = 13.sp,
        previewDescriptionLineHeight = 18.sp,
        previewDescriptionMaxLines = Int.MAX_VALUE,

        loadingTextFontSize = 14.sp,
        loadingTextLineHeight = 18.sp,

        primaryButtonHeight = 58.dp,
        primaryButtonCornerRadius = 24.dp,
        primaryButtonShadowElevation = 8.dp,
        primaryButtonFontSize = 16.sp,
        primaryButtonLineHeight = 20.sp,

        resetButtonHeight = 54.dp,
        resetButtonCornerRadius = 22.dp,
        resetButtonShadowElevation = 5.dp,
        resetButtonFontSize = 15.sp,
        resetButtonLineHeight = 19.sp
    )
}

private fun squarePremiumProfileBackgroundMetrics(): ProfileBackgroundLayoutMetrics {
    return ProfileBackgroundLayoutMetrics(
        contentHorizontalPadding = 16.dp,
        contentTopPadding = 12.dp,
        contentBottomPadding = 10.dp,
        contentSpacing = 8.dp,

        titleFontSize = 18.sp,
        titleLineHeight = 22.sp,
        subtitleFontSize = 11.sp,
        subtitleLineHeight = 15.sp,
        subtitleMaxLines = 1,

        previewCardCornerRadius = 24.dp,
        previewCardShadowElevation = 5.dp,
        previewCardPadding = 12.dp,
        previewTitleFontSize = 13.sp,
        previewTitleLineHeight = 16.sp,
        previewTitleMaxLines = 1,
        previewTitleToImageSpacing = 9.dp,

        previewImageHeight = 154.dp,
        previewImageCornerRadius = 26.dp,
        previewImageShadowElevation = 8.dp,

        previewImageToDescriptionSpacing = 9.dp,
        previewDescriptionFontSize = 11.sp,
        previewDescriptionLineHeight = 14.sp,
        previewDescriptionMaxLines = 2,

        loadingTextFontSize = 12.sp,
        loadingTextLineHeight = 15.sp,

        primaryButtonHeight = 44.dp,
        primaryButtonCornerRadius = 18.dp,
        primaryButtonShadowElevation = 5.dp,
        primaryButtonFontSize = 14.sp,
        primaryButtonLineHeight = 17.sp,

        resetButtonHeight = 38.dp,
        resetButtonCornerRadius = 16.dp,
        resetButtonShadowElevation = 0.dp,
        resetButtonFontSize = 12.sp,
        resetButtonLineHeight = 15.sp
    )
}

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

    when (rememberChaiOkDeviceClass()) {
        ChaiOkDeviceClass.SquareCompact -> {
            ProfileBackgroundSquarePremiumScreen(
                selectedBackground = selectedBackground,
                hasCustomImage = hasCustomImage,
                onBack = onBack,
                onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                onReset = { onSelect(DEFAULT_BACKGROUND) }
            )
        }

        ChaiOkDeviceClass.Regular -> {
            ProfileBackgroundRegularScreen(
                selectedBackground = selectedBackground,
                hasCustomImage = hasCustomImage,
                onBack = onBack,
                onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                onReset = { onSelect(DEFAULT_BACKGROUND) }
            )
        }
    }
}

@Composable
private fun ProfileBackgroundRegularScreen(
    selectedBackground: String?,
    hasCustomImage: Boolean,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    onReset: () -> Unit
) {
    val metrics = regularProfileBackgroundMetrics()
    val scrollState = rememberScrollState()

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = metrics.contentHorizontalPadding)
                    .padding(
                        top = metrics.contentTopPadding,
                        bottom = metrics.contentBottomPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(metrics.contentSpacing)
            ) {
                Text(
                    text = "Фон официанта",
                    color = BackgroundPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.titleFontSize,
                    lineHeight = metrics.titleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Выберите изображение из файлового менеджера. Оно будет использоваться как фон карточки официанта.",
                    color = BackgroundSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = metrics.subtitleFontSize,
                    lineHeight = metrics.subtitleLineHeight,
                    maxLines = metrics.subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )

                BackgroundPreviewCard(
                    selectedBackground = selectedBackground,
                    hasCustomImage = hasCustomImage,
                    metrics = metrics,
                    premium = false
                )

                PickImageButton(
                    text = if (hasCustomImage) {
                        "Выбрать другое изображение"
                    } else {
                        "Выбрать изображение"
                    },
                    metrics = metrics,
                    premium = false,
                    onClick = onPickImage
                )

                if (hasCustomImage) {
                    ResetBackgroundButton(
                        metrics = metrics,
                        premium = false,
                        onClick = onReset
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBackgroundSquarePremiumScreen(
    selectedBackground: String?,
    hasCustomImage: Boolean,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    onReset: () -> Unit
) {
    val metrics = squarePremiumProfileBackgroundMetrics()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundScreenColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TiplyBackTopAppBar(
                title = "Фон профиля",
                onBack = onBack,
                elevation = 10.dp,
                ambientAlpha = 0.16f,
                spotAlpha = 0.22f
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = metrics.contentHorizontalPadding)
                    .padding(
                        top = metrics.contentTopPadding,
                        bottom = 0.dp
                    )
            ) {
                Text(
                    text = "Оформление",
                    color = BackgroundPrimaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.titleFontSize,
                    lineHeight = metrics.titleLineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Фон карточки официанта на главном экране",
                    color = BackgroundSecondaryTextColor,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = metrics.subtitleFontSize,
                    lineHeight = metrics.subtitleLineHeight,
                    maxLines = metrics.subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(metrics.contentSpacing))

                BackgroundPreviewCard(
                    selectedBackground = selectedBackground,
                    hasCustomImage = hasCustomImage,
                    metrics = metrics,
                    premium = true
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = metrics.contentHorizontalPadding)
                    .padding(bottom = metrics.contentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PickImageButton(
                    text = if (hasCustomImage) {
                        "Выбрать другое изображение"
                    } else {
                        "Выбрать изображение"
                    },
                    metrics = metrics,
                    premium = true,
                    onClick = onPickImage
                )

                if (hasCustomImage) {
                    ResetBackgroundButton(
                        metrics = metrics,
                        premium = true,
                        onClick = onReset
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundPreviewCard(
    selectedBackground: String?,
    hasCustomImage: Boolean,
    metrics: ProfileBackgroundLayoutMetrics,
    premium: Boolean
) {
    val cardBackground = if (premium) {
        Color.White
    } else {
        BackgroundCardColor
    }

    val borderColor = when {
        !premium -> Color.Transparent
        selectedBackground == null -> BackgroundStrokeColor.copy(alpha = 0.86f)
        hasCustomImage -> BackgroundAccentColor.copy(alpha = 0.22f)
        else -> BackgroundGreenColor.copy(alpha = 0.18f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = metrics.previewCardShadowElevation,
                shape = RoundedCornerShape(metrics.previewCardCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (premium) 0.055f else 0.10f),
                spotColor = Color.Black.copy(alpha = if (premium) 0.10f else 0.16f)
            )
            .clip(RoundedCornerShape(metrics.previewCardCornerRadius))
            .background(cardBackground)
            .border(
                width = if (premium) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(metrics.previewCardCornerRadius)
            )
            .padding(metrics.previewCardPadding)
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
            fontSize = metrics.previewTitleFontSize,
            lineHeight = metrics.previewTitleLineHeight,
            maxLines = metrics.previewTitleMaxLines,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(metrics.previewTitleToImageSpacing))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.previewImageHeight)
                .shadow(
                    elevation = metrics.previewImageShadowElevation,
                    shape = RoundedCornerShape(metrics.previewImageCornerRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = if (premium) 0.12f else 0.18f),
                    spotColor = Color.Black.copy(alpha = if (premium) 0.20f else 0.28f)
                )
                .clip(RoundedCornerShape(metrics.previewImageCornerRadius))
                .background(Color(0xFFF0F2F4)),
            contentAlignment = Alignment.Center
        ) {
            when {
                selectedBackground == null -> {
                    LoadingBackgroundPreview(
                        text = "Загрузка фона...",
                        metrics = metrics,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                hasCustomImage -> {
                    UriPreviewImage(
                        uriString = selectedBackground,
                        metrics = metrics,
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

        Spacer(modifier = Modifier.height(metrics.previewImageToDescriptionSpacing))

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
            fontWeight = if (premium) FontWeight.Medium else FontWeight.Normal,
            fontSize = metrics.previewDescriptionFontSize,
            lineHeight = metrics.previewDescriptionLineHeight,
            maxLines = metrics.previewDescriptionMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UriPreviewImage(
    uriString: String,
    metrics: ProfileBackgroundLayoutMetrics,
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
                metrics = metrics,
                modifier = modifier
            )
        }

        PreviewImageState.Failed -> {
            LoadingBackgroundPreview(
                text = "Не удалось загрузить изображение",
                metrics = metrics,
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
    metrics: ProfileBackgroundLayoutMetrics,
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
            fontSize = metrics.loadingTextFontSize,
            lineHeight = metrics.loadingTextLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun PickImageButton(
    text: String,
    metrics: ProfileBackgroundLayoutMetrics,
    premium: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.primaryButtonHeight)
            .shadow(
                elevation = metrics.primaryButtonShadowElevation,
                shape = RoundedCornerShape(metrics.primaryButtonCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (premium) 0.08f else 0.12f),
                spotColor = Color.Black.copy(alpha = if (premium) 0.14f else 0.18f)
            )
            .clip(RoundedCornerShape(metrics.primaryButtonCornerRadius))
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
            fontSize = metrics.primaryButtonFontSize,
            lineHeight = metrics.primaryButtonLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun ResetBackgroundButton(
    metrics: ProfileBackgroundLayoutMetrics,
    premium: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor = if (premium) {
        Color.White
    } else {
        Color(0xFFF7F8FA)
    }

    val borderColor = if (premium) {
        BackgroundStrokeColor.copy(alpha = 0.9f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.resetButtonHeight)
            .shadow(
                elevation = metrics.resetButtonShadowElevation,
                shape = RoundedCornerShape(metrics.resetButtonCornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (premium) 0f else 0.06f),
                spotColor = Color.Black.copy(alpha = if (premium) 0f else 0.10f)
            )
            .clip(RoundedCornerShape(metrics.resetButtonCornerRadius))
            .background(backgroundColor)
            .border(
                width = if (premium) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(metrics.resetButtonCornerRadius)
            )
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
            fontSize = metrics.resetButtonFontSize,
            lineHeight = metrics.resetButtonLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
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
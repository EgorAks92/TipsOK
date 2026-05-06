package com.chaiok.pos.presentation.pc

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chaiok.pos.R
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.UpdatePcIdleImagesUseCase
import com.chaiok.pos.presentation.components.TiplyBackTopAppBar
import com.chaiok.pos.presentation.theme.MontserratFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PcIdleImagesUiState(val images: List<String> = emptyList())

class PcIdleImagesViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updatePcIdleImagesUseCase: UpdatePcIdleImagesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(PcIdleImagesUiState())
    val uiState: StateFlow<PcIdleImagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(images = settings.pcIdleImages) }
            }
        }
    }

    fun setImages(images: List<String>) = viewModelScope.launch {
        updatePcIdleImagesUseCase(images.map { it.trim() }.filter { it.isNotBlank() }.distinct())
    }

    fun addImages(images: List<String>) {
        setImages((_uiState.value.images + images).distinct())
    }

    fun removeImage(image: String) = setImages(_uiState.value.images.filterNot { it == image })
    fun reset() = setImages(emptyList())
}

@Composable
fun PcIdleImagesRoute(viewModel: PcIdleImagesViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PcIdleImagesScreen(
        state = state,
        onBack = onBack,
        onAddImages = viewModel::addImages,
        onRemoveImage = viewModel::removeImage,
        onReset = viewModel::reset
    )
}

@Composable
fun PcIdleImagesScreen(
    state: PcIdleImagesUiState,
    onBack: () -> Unit,
    onAddImages: (List<String>) -> Unit,
    onRemoveImage: (String) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val prepared = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri.toString()
            }.getOrNull()
        }
        if (prepared.isNotEmpty()) onAddImages(prepared)
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        TiplyBackTopAppBar(title = "Экран ожидания кассы", onBack = onBack)
        Button(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.padding(16.dp)) {
            Text("Добавить изображения")
        }
        if (state.images.isNotEmpty()) {
            Button(onClick = onReset, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Сбросить к умолчанию") }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.images, key = { it }) { image ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF4F6F8)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PcIdleImagePreview(image, Modifier.size(92.dp).clip(RoundedCornerShape(12.dp)))
                    Text(
                        text = "Удалить",
                        color = Color(0xFFD32F2F),
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp).clickable { onRemoveImage(image) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PcIdleImagePreview(image: String, modifier: Modifier) {
    if (image.startsWith("content://", true) || image.startsWith("file://", true)) {
        val context = LocalContext.current
        val bitmap by produceState<ImageBitmap?>(null, image) { value = loadImageBitmapFromUri(context, image) }
        if (bitmap != null) {
            Image(bitmap = bitmap!!, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
        } else {
            FailedPlaceholder(modifier)
        }
    } else {
        Image(painter = painterResource(R.drawable.waiter_card_background), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

@Composable
private fun FailedPlaceholder(modifier: Modifier) {
    Box(modifier = modifier.background(Brush.linearGradient(listOf(Color(0xFF1B263B), Color(0xFF415A77)))), contentAlignment = Alignment.Center) {
        Text("Не удалось загрузить изображение", color = Color.White, fontFamily = MontserratFontFamily, fontSize = 10.sp)
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

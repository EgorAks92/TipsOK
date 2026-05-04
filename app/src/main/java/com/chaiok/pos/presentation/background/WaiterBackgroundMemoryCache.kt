package com.chaiok.pos.presentation.background

import androidx.compose.ui.graphics.ImageBitmap
import java.util.concurrent.ConcurrentHashMap

object WaiterBackgroundMemoryCache {
    private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

    @Volatile
    var currentBackground: String? = null
        private set

    fun setCurrentBackground(background: String?) {
        currentBackground = background
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun getImage(uriString: String): ImageBitmap? {
        return imageCache[uriString]
    }

    fun putImage(uriString: String, bitmap: ImageBitmap) {
        imageCache[uriString] = bitmap
    }

    fun clearImage(uriString: String) {
        imageCache.remove(uriString)
    }
}
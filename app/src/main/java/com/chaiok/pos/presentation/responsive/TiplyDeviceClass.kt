package com.chaiok.pos.presentation.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TiplyDeviceClass {
    SquareCompact,
    CompactPortrait,
    Regular
}

data class TiplyScreenMetrics(
    val width: Dp,
    val height: Dp,
    val deviceClass: TiplyDeviceClass
) {
    val isSquareCompact: Boolean
        get() = deviceClass == TiplyDeviceClass.SquareCompact

    val isCompactPortrait: Boolean
        get() = deviceClass == TiplyDeviceClass.CompactPortrait

    val isCompact: Boolean
        get() = deviceClass == TiplyDeviceClass.SquareCompact ||
                deviceClass == TiplyDeviceClass.CompactPortrait
}

@Composable
fun rememberTiplyScreenMetrics(): TiplyScreenMetrics {
    val configuration = LocalConfiguration.current

    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp

    val deviceClass = when {
        width <= 520.dp && height <= 520.dp -> {
            TiplyDeviceClass.SquareCompact
        }

        height < 780.dp -> {
            TiplyDeviceClass.CompactPortrait
        }

        else -> {
            TiplyDeviceClass.Regular
        }
    }

    return TiplyScreenMetrics(
        width = width,
        height = height,
        deviceClass = deviceClass
    )
}
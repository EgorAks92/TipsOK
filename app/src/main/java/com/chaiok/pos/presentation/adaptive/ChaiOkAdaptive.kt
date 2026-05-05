package com.chaiok.pos.presentation.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

enum class ChaiOkDeviceClass {
    Regular,
    SquareCompact
}

val ChaiOkDeviceClass.isSquareCompact: Boolean
    get() = this == ChaiOkDeviceClass.SquareCompact

@Composable
fun rememberChaiOkDeviceClass(): ChaiOkDeviceClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    return remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp
    ) {
        if (screenWidth <= 520.dp && screenHeight <= 520.dp) {
            ChaiOkDeviceClass.SquareCompact
        } else {
            ChaiOkDeviceClass.Regular
        }
    }
}
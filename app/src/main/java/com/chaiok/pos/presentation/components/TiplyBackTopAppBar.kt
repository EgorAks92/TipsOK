package com.chaiok.pos.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
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
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private data class TiplyBackTopAppBarMetrics(
    val height: Dp,
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,

    val bottomCornerRadius: Dp,

    val resolvedElevation: Dp,
    val resolvedAmbientAlpha: Float,
    val resolvedSpotAlpha: Float,

    val titleHorizontalPadding: Dp,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,

    val touchSize: Dp,
    val iconSize: Dp
)

private fun regularTiplyBackTopAppBarMetrics(
    elevation: Dp,
    ambientAlpha: Float,
    spotAlpha: Float
): TiplyBackTopAppBarMetrics {
    return TiplyBackTopAppBarMetrics(
        height = 72.dp,
        horizontalPadding = 32.dp,
        topPadding = 10.dp,
        bottomPadding = 10.dp,

        bottomCornerRadius = 46.dp,

        resolvedElevation = elevation,
        resolvedAmbientAlpha = ambientAlpha,
        resolvedSpotAlpha = spotAlpha,

        titleHorizontalPadding = 56.dp,
        titleFontSize = 18.sp,
        titleLineHeight = 22.sp,

        touchSize = 48.dp,
        iconSize = 30.dp
    )
}

private fun squareCompactTiplyBackTopAppBarMetrics(
    elevation: Dp,
    ambientAlpha: Float,
    spotAlpha: Float
): TiplyBackTopAppBarMetrics {
    return TiplyBackTopAppBarMetrics(
        height = 54.dp,
        horizontalPadding = 20.dp,
        topPadding = 6.dp,
        bottomPadding = 7.dp,

        bottomCornerRadius = 30.dp,

        resolvedElevation = minOf(elevation, 10.dp),
        resolvedAmbientAlpha = minOf(ambientAlpha, 0.18f),
        resolvedSpotAlpha = minOf(spotAlpha, 0.24f),

        titleHorizontalPadding = 46.dp,
        titleFontSize = 16.sp,
        titleLineHeight = 19.sp,

        touchSize = 40.dp,
        iconSize = 24.dp
    )
}

@Composable
fun TiplyBackTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = 22.dp,
    ambientAlpha: Float = 0.20f,
    spotAlpha: Float = 0.28f,
    iconTint: Color = Color(0xFF1B2128),
    titleColor: Color = Color(0xFF1B2128)
) {
    val deviceClass = rememberChaiOkDeviceClass()

    val metrics = when (deviceClass) {
        ChaiOkDeviceClass.SquareCompact -> {
            squareCompactTiplyBackTopAppBarMetrics(
                elevation = elevation,
                ambientAlpha = ambientAlpha,
                spotAlpha = spotAlpha
            )
        }

        ChaiOkDeviceClass.Regular -> {
            regularTiplyBackTopAppBarMetrics(
                elevation = elevation,
                ambientAlpha = ambientAlpha,
                spotAlpha = spotAlpha
            )
        }
    }

    TiplyBackTopAppBarContent(
        title = title,
        onBack = onBack,
        modifier = modifier,
        metrics = metrics,
        iconTint = iconTint,
        titleColor = titleColor
    )
}

@Composable
private fun TiplyBackTopAppBarContent(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier,
    metrics: TiplyBackTopAppBarMetrics,
    iconTint: Color,
    titleColor: Color
) {
    val barShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = metrics.bottomCornerRadius,
        bottomEnd = metrics.bottomCornerRadius
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.height)
            .shadow(
                elevation = metrics.resolvedElevation,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = metrics.resolvedAmbientAlpha),
                spotColor = Color.Black.copy(alpha = metrics.resolvedSpotAlpha)
            )
            .clip(barShape)
            .background(Color.White)
            .padding(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                top = metrics.topPadding,
                bottom = metrics.bottomPadding
            )
    ) {
        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = metrics.titleHorizontalPadding),
            color = titleColor,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.titleFontSize,
            lineHeight = metrics.titleLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .size(metrics.touchSize)
                .align(Alignment.CenterStart)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings_back),
                contentDescription = "Назад",
                modifier = Modifier.size(metrics.iconSize),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(iconTint)
            )
        }
    }
}
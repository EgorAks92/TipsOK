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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private data class TiplyBackTopAppBarMetrics(
    val isSquareCompact: Boolean,
    val height: Dp,
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val bottomRadius: Dp,
    val shadowElevation: Dp,
    val titleHorizontalPadding: Dp,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val backTouchSize: Dp,
    val backIconSize: Dp
)

@Composable
private fun tiplyBackTopAppBarMetrics(
    regularElevation: Dp
): TiplyBackTopAppBarMetrics {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSquareCompact = screenWidth <= 520.dp && screenHeight <= 520.dp

    return if (isSquareCompact) {
        TiplyBackTopAppBarMetrics(
            isSquareCompact = true,
            height = 54.dp,
            horizontalPadding = 20.dp,
            topPadding = 6.dp,
            bottomPadding = 6.dp,
            bottomRadius = 30.dp,
            shadowElevation = 8.dp,
            titleHorizontalPadding = 48.dp,
            titleFontSize = 16.sp,
            titleLineHeight = 20.sp,
            backTouchSize = 42.dp,
            backIconSize = 24.dp
        )
    } else {
        TiplyBackTopAppBarMetrics(
            isSquareCompact = false,
            height = 72.dp,
            horizontalPadding = 32.dp,
            topPadding = 10.dp,
            bottomPadding = 10.dp,
            bottomRadius = 46.dp,
            shadowElevation = regularElevation,
            titleHorizontalPadding = 56.dp,
            titleFontSize = 18.sp,
            titleLineHeight = 22.sp,
            backTouchSize = 48.dp,
            backIconSize = 30.dp
        )
    }
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
    val metrics = tiplyBackTopAppBarMetrics(
        regularElevation = elevation
    )

    val barShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = metrics.bottomRadius,
        bottomEnd = metrics.bottomRadius
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.height)
            .shadow(
                elevation = metrics.shadowElevation,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = ambientAlpha),
                spotColor = Color.Black.copy(alpha = spotAlpha)
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
                .size(metrics.backTouchSize)
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
                modifier = Modifier.size(metrics.backIconSize),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(iconTint)
            )
        }
    }
}
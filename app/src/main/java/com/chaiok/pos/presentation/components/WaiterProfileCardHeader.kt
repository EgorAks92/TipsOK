package com.chaiok.pos.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

private data class WaiterProfileHeaderMetrics(
    val profileSpacer: Dp,
    val avatarContainerSize: Dp,
    val avatarImageSize: Dp,
    val avatarRadius: Dp,
    val nameSize: Int,
    val nameStatusSpacer: Dp,
    val statusSize: Int,
    val headerHeight: Dp,
    val waiterCardTopPadding: Dp,
    val waiterCardHeight: Dp,
    val waiterCardHorizontalPadding: Dp,
    val waiterCardCutoutPadding: Dp
)

@Composable
fun WaiterProfileCardHeader(
    waiterName: String,
    waiterStatus: String,
    modifier: Modifier = Modifier,
    backgroundRes: Int = R.drawable.waiter_card_background,
    avatarRes: Int = R.drawable.ic_waiter_avatar
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 780.dp

    val metrics = if (isCompact) {
        WaiterProfileHeaderMetrics(
            profileSpacer = 0.dp,
            avatarContainerSize = 70.dp,
            avatarImageSize = 62.dp,
            avatarRadius = 22.dp,
            nameSize = 16,
            nameStatusSpacer = 0.dp,
            statusSize = 14,
            headerHeight = 224.dp,
            waiterCardTopPadding = 16.dp,
            waiterCardHeight = 178.dp,
            waiterCardHorizontalPadding = 32.dp,
            waiterCardCutoutPadding = 12.dp
        )
    } else {
        WaiterProfileHeaderMetrics(
            profileSpacer = 0.dp,
            avatarContainerSize = 72.dp,
            avatarImageSize = 64.dp,
            avatarRadius = 22.dp,
            nameSize = 16,
            nameStatusSpacer = 8.dp,
            statusSize = 14,
            headerHeight = 242.dp,
            waiterCardTopPadding = 16.dp,
            waiterCardHeight = 196.dp,
            waiterCardHorizontalPadding = 16.dp,
            waiterCardCutoutPadding = 12.dp
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.headerHeight)
        ) {
            WaiterBackgroundCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = metrics.waiterCardTopPadding),
                cardHeight = metrics.waiterCardHeight,
                horizontalPadding = metrics.waiterCardHorizontalPadding,
                avatarSize = metrics.avatarContainerSize,
                avatarRadius = metrics.avatarRadius,
                cutoutPadding = metrics.waiterCardCutoutPadding,
                backgroundRes = backgroundRes
            )

            WaiterAvatar(
                metrics = metrics,
                avatarRes = avatarRes,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = metrics.waiterCardTopPadding +
                                metrics.waiterCardHeight -
                                (metrics.avatarContainerSize * 0.64f)
                    )
            )
        }

        Spacer(modifier = Modifier.height(metrics.profileSpacer))

        Text(
            text = waiterName.ifBlank { "Ваш официант" },
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.nameSize.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(metrics.nameStatusSpacer))

        Text(
            text = waiterStatus.ifBlank { "Коплю на отпуск!" },
            color = Color(0xFF3B4148),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.statusSize.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WaiterBackgroundCard(
    modifier: Modifier = Modifier,
    cardHeight: Dp,
    horizontalPadding: Dp,
    avatarSize: Dp,
    avatarRadius: Dp,
    cutoutPadding: Dp,
    backgroundRes: Int
) {
    val cardShape = WaiterCardAvatarCutoutShape(
        cornerRadius = 32.dp,
        avatarSize = avatarSize,
        avatarRadius = avatarRadius,
        cutoutPadding = cutoutPadding
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(cardHeight)
            .shadow(
                elevation = 14.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.64f),
                spotColor = Color.Black.copy(alpha = 0.72f)
            )
            .clip(cardShape)
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun WaiterAvatar(
    metrics: WaiterProfileHeaderMetrics,
    avatarRes: Int,
    modifier: Modifier = Modifier
) {
    val avatarShape = RoundedCornerShape(metrics.avatarRadius)

    Box(
        modifier = modifier
            .size(metrics.avatarContainerSize)
            .shadow(
                elevation = 8.dp,
                shape = avatarShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(avatarShape)
            .background(Color(0xFFF2F3F2)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = "Аватар официанта",
            modifier = Modifier.size(metrics.avatarImageSize),
            contentScale = ContentScale.Fit
        )
    }
}

private class WaiterCardAvatarCutoutShape(
    private val cornerRadius: Dp,
    private val avatarSize: Dp,
    private val avatarRadius: Dp,
    private val cutoutPadding: Dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        with(density) {
            val width = size.width
            val height = size.height

            val cardRadius = cornerRadius.toPx()
                .coerceAtMost(width / 2f)
                .coerceAtMost(height / 2f)

            val gap = cutoutPadding.toPx()
            val cutoutWidth = avatarSize.toPx() + gap * 2f
            val cutoutDepth = avatarSize.toPx() * 0.64f + gap
            val cutoutRadius = avatarRadius.toPx() + gap

            val centerX = width / 2f

            val cutoutLeft = (centerX - cutoutWidth / 2f)
                .coerceAtLeast(cardRadius)

            val cutoutRight = (centerX + cutoutWidth / 2f)
                .coerceAtMost(width - cardRadius)

            val cutoutTop = (height - cutoutDepth)
                .coerceAtLeast(cardRadius)

            val bottomCutoutRadius = 24.dp.toPx()

            path.moveTo(cardRadius, 0f)

            path.lineTo(width - cardRadius, 0f)
            path.quadraticBezierTo(width, 0f, width, cardRadius)

            path.lineTo(width, height - cardRadius)
            path.quadraticBezierTo(width, height, width - cardRadius, height)

            path.lineTo(cutoutRight + bottomCutoutRadius, height)

            path.quadraticBezierTo(
                cutoutRight,
                height,
                cutoutRight,
                height - bottomCutoutRadius
            )

            path.lineTo(cutoutRight, cutoutTop + cutoutRadius)

            path.quadraticBezierTo(
                cutoutRight,
                cutoutTop,
                cutoutRight - cutoutRadius,
                cutoutTop
            )

            path.lineTo(cutoutLeft + cutoutRadius, cutoutTop)

            path.quadraticBezierTo(
                cutoutLeft,
                cutoutTop,
                cutoutLeft,
                cutoutTop + cutoutRadius
            )

            path.lineTo(cutoutLeft, height - bottomCutoutRadius)

            path.quadraticBezierTo(
                cutoutLeft,
                height,
                cutoutLeft - bottomCutoutRadius,
                height
            )

            path.lineTo(cardRadius, height)
            path.quadraticBezierTo(0f, height, 0f, height - cardRadius)

            path.lineTo(0f, cardRadius)
            path.quadraticBezierTo(0f, 0f, cardRadius, 0f)

            path.close()
        }

        return Outline.Generic(path)
    }
}
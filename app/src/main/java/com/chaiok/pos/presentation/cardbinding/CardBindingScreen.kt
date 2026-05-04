package com.chaiok.pos.presentation.cardbinding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiok.pos.R
import com.chaiok.pos.presentation.theme.MontserratFontFamily

@Composable
fun CardBindingScreen(
    state: CardBindingUiState,
    onBack: () -> Unit,
    onReadCard: () -> Unit
) {
    val pulseScale = animateFloatAsState(
        targetValue = when (state.status) {
            CardBindingStatus.Success, CardBindingStatus.Error -> 1.25f
            else -> 1f
        },
        animationSpec = tween(420),
        label = "card_binding_state_scale"
    )

    Scaffold(topBar = { CardBindingTopAppBar(onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Text(state.message, style = MaterialTheme.typography.titleMedium)

            if (state.status == CardBindingStatus.Reading) {
                CircularProgressIndicator(modifier = Modifier.size(84.dp), strokeWidth = 6.dp)
            }

            AnimatedVisibility(state.status == CardBindingStatus.Success) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF24A148),
                    modifier = Modifier
                        .size(128.dp)
                        .graphicsLayer(scaleX = pulseScale.value, scaleY = pulseScale.value)
                )
            }
            AnimatedVisibility(state.status == CardBindingStatus.Error) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(128.dp)
                        .graphicsLayer(scaleX = pulseScale.value, scaleY = pulseScale.value)
                )
            }

            Button(onClick = onReadCard, modifier = Modifier.fillMaxWidth(), enabled = state.status != CardBindingStatus.Reading) {
                Text(if (state.status == CardBindingStatus.Reading) "Чтение..." else "Считать карту")
            }
        }
    }
}


@Composable
private fun CardBindingTopAppBar(onBack: () -> Unit) {
    val barShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 46.dp,
        bottomEnd = 46.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 22.dp,
                shape = barShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.20f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(barShape)
            .background(Color.White)
            .padding(start = 32.dp, end = 32.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Text(
            text = "Привязка карты",
            modifier = Modifier.align(Alignment.Center),
            color = Color(0xFF1B2128),
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings_back),
                contentDescription = "Назад",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color(0xFF1B2128))
            )
        }
    }
}

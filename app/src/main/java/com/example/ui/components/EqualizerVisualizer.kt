package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EqualizerVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 16.dp,
    color: Color = EmeraldPrimary
) {
    val bar1 = remember { Animatable(0.3f) }
    val bar2 = remember { Animatable(0.7f) }
    val bar3 = remember { Animatable(0.5f) }
    val bar4 = remember { Animatable(0.8f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            launch {
                bar1.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            delay(100)
            launch {
                bar2.animateTo(
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(350, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            delay(80)
            launch {
                bar3.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(450, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            delay(120)
            launch {
                bar4.animateTo(
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(380, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        } else {
            bar1.snapTo(0.3f)
            bar2.snapTo(0.2f)
            bar3.snapTo(0.4f)
            bar4.snapTo(0.2f)
        }
    }

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(maxHeight * bar1.value)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(maxHeight * bar2.value)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(maxHeight * bar3.value)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(maxHeight * bar4.value)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

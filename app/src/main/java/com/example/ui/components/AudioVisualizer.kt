package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VibrantPink

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 3.dp,
    maxHeight: Dp = 36.dp,
    brush: Brush = Brush.verticalGradient(listOf(NeonCyan, VibrantPink))
) {
    val transition = rememberInfiniteTransition(label = "visualizer")

    // Generate staggered animated heights for bars
    val anim1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val anim2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val anim3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val anim4 by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 390, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val heights = listOf(anim1, anim2, anim3, anim4, anim2, anim1, anim4, anim3)

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val hFraction = if (isPlaying) heights[i % heights.size] else 0.15f
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(hFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(brush)
            )
        }
    }
}

package dev.liquidglass.sample

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Backdrop = Color(0xFF101023)
private val BlobColors = listOf(
    Color(0xFFFF5E8E), Color(0xFF7C4DFF), Color(0xFF00E5C3),
    Color(0xFFFFB300), Color(0xFF40C4FF),
)

private data class DemoCard(val emoji: String, val title: String, val body: String)

private val DemoCards = listOf(
    DemoCard("🌊", "Refraction", "Drag the lens around — the rim bends the world like a convex slab of glass."),
    DemoCard("🫧", "Liquid merge", "Open the + cluster and watch the actions melt out of one another."),
    DemoCard("✨", "Specular rim", "A thin angle-aware highlight hugs every contour, brighter while pressed."),
    DemoCard("🧪", "Three tiers", "Full AGSL on Android 13+, frosted blur on 12, translucent scrim back to 5.0."),
    DemoCard("🎛️", "Composable", "One modifier on any composable. No Material dependency, any design system."),
    DemoCard("🏝️", "Scroll under", "Everything here scrolls beneath the bar and the glass reacts live."),
)

/** A vivid animated backdrop: drifting gradient blobs under scrolling cards. */
@Composable
internal fun DemoBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Backdrop)) {
        DriftingBlobs(Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 120.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(DemoCards) { card -> BackdropCard(card) }
        }
    }
}

@Composable
private fun DriftingBlobs(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "blobs")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    Canvas(modifier) {
        val maxRadius = size.minDimension * 0.55f
        BlobColors.forEachIndexed { index, color ->
            val angle = drift.value + index * (2f * Math.PI.toFloat() / BlobColors.size)
            val cx = size.width * (0.5f + 0.42f * cos(angle + index))
            val cy = size.height * (0.5f + 0.42f * sin(angle * 0.8f + index * 1.7f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = maxRadius,
                ),
                radius = maxRadius,
                center = Offset(cx, cy),
            )
        }
    }
}

@Composable
private fun BackdropCard(card: DemoCard) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "${card.emoji}  ${card.title}",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = card.body,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

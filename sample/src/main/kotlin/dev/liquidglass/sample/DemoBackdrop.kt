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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// A bright, saturated base so the glass samples vivid color, not near-black.
private val BaseGradient = listOf(
    Color(0xFF2A1378), // deep indigo
    Color(0xFF7A1FA2), // violet
    Color(0xFFB3146B), // magenta
)
private val BlobColors = listOf(
    Color(0xFFFF477E), Color(0xFF8B5CFF), Color(0xFF00E5C3),
    Color(0xFFFFC130), Color(0xFF34C8FF),
)

private data class DemoCard(val emoji: String, val title: String, val body: String, val accent: Color)

private val DemoCards = listOf(
    DemoCard("🌊", "Refraction", "Drag the lens around — the rim bends the world like a convex slab of glass.", Color(0xFF34C8FF)),
    DemoCard("🫧", "Liquid merge", "Open the + cluster and watch the actions melt out of one another.", Color(0xFF00E5C3)),
    DemoCard("✨", "Specular rim", "A thin angle-aware highlight hugs every contour, brighter while pressed.", Color(0xFFFFC130)),
    DemoCard("🧪", "Three tiers", "Full AGSL on Android 13+, frosted blur on 12, translucent scrim back to 5.0.", Color(0xFFFF477E)),
    DemoCard("🎛️", "Composable", "One modifier on any composable. No Material dependency, any design system.", Color(0xFF8B5CFF)),
    DemoCard("🏝️", "Scroll under", "Everything here scrolls beneath the bar and the glass reacts live.", Color(0xFF34C8FF)),
)

/**
 * A vivid, high-frequency backdrop. Glass over flat dark color has nothing to
 * refract; here drifting saturated blobs and bold bright rings give the lens
 * sharp, colorful edges to visibly bend.
 */
@Composable
internal fun DemoBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = BaseGradient,
                start = Offset.Zero,
                end = Offset.Infinite,
            ),
        ),
    ) {
        AnimatedField(Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 120.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(DemoCards) { card -> BackdropCard(card) }
        }
    }
}

@Composable
private fun AnimatedField(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "field")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    Canvas(modifier) {
        drawBlobs(drift)
        drawConcentricRings(drift)
    }
}

private fun DrawScope.drawBlobs(drift: Float) {
    val maxRadius = size.minDimension * 0.6f
    BlobColors.forEachIndexed { index, color ->
        val angle = drift + index * (2f * Math.PI.toFloat() / BlobColors.size)
        val cx = size.width * (0.5f + 0.42f * cos(angle + index))
        val cy = size.height * (0.5f + 0.42f * sin(angle * 0.8f + index * 1.7f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                center = Offset(cx, cy),
                radius = maxRadius,
            ),
            radius = maxRadius,
            center = Offset(cx, cy),
        )
    }
}

/**
 * Bold bright rings — the high-frequency element. Sharp concentric edges are
 * exactly what a convex lens distorts most dramatically, so they make the
 * refraction unmistakable wherever the glass passes over them.
 */
private fun DrawScope.drawConcentricRings(drift: Float) {
    val center = Offset(
        size.width * (0.5f + 0.18f * cos(drift)),
        size.height * (0.5f + 0.12f * sin(drift * 1.3f)),
    )
    val ringCount = 9
    val step = size.minDimension * 0.07f
    repeat(ringCount) { i ->
        val radius = step * (i + 1)
        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = radius,
            center = center,
            style = Stroke(width = 3f),
        )
    }
}

@Composable
private fun BackdropCard(card: DemoCard) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        card.accent.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                RoundedCornerShape(20.dp),
            )
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
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

package dev.liquidglass.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlassShapeTest {

    private val density = Density(density = 2f, fontScale = 1f)

    @Test
    fun `capsule resolves to half the smaller dimension`() {
        val radius = GlassShape.Capsule.resolveCornerRadiusPx(Size(200f, 80f), density)
        assertThat(radius).isEqualTo(40f)
    }

    @Test
    fun `circle on a square resolves to half the side`() {
        val radius = GlassShape.Circle.resolveCornerRadiusPx(Size(96f, 96f), density)
        assertThat(radius).isEqualTo(48f)
    }

    @Test
    fun `rounded rectangle converts dp through density`() {
        val radius = GlassShape.RoundedRectangle(12.dp)
            .resolveCornerRadiusPx(Size(400f, 200f), density)
        assertThat(radius).isEqualTo(24f)
    }

    @Test
    fun `rounded rectangle radius is clamped to half the smaller dimension`() {
        val radius = GlassShape.RoundedRectangle(100.dp)
            .resolveCornerRadiusPx(Size(400f, 60f), density)
        assertThat(radius).isEqualTo(30f)
    }

    @Test
    fun `negative corner radius is rejected`() {
        try {
            GlassShape.RoundedRectangle((-1).dp)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessageThat().contains("cornerRadius")
        }
    }
}

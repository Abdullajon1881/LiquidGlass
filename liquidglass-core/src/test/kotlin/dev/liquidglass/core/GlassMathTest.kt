package dev.liquidglass.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlassMathTest {

    // -- sdRoundedBox ------------------------------------------------------

    @Test
    fun `center of a rounded box is the negative distance to the nearest edge`() {
        val sd = GlassMath.sdRoundedBox(0f, 0f, halfWidth = 50f, halfHeight = 25f, cornerRadius = 10f)
        assertThat(sd).isWithin(1e-4f).of(-25f)
    }

    @Test
    fun `points on the flat edges sit exactly on the surface`() {
        val right = GlassMath.sdRoundedBox(50f, 0f, 50f, 25f, 10f)
        val top = GlassMath.sdRoundedBox(0f, -25f, 50f, 25f, 10f)
        assertThat(right).isWithin(1e-4f).of(0f)
        assertThat(top).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `outside distance grows linearly past a flat edge`() {
        val sd = GlassMath.sdRoundedBox(60f, 0f, 50f, 25f, 10f)
        assertThat(sd).isWithin(1e-4f).of(10f)
    }

    @Test
    fun `a capsule surface passes through the cap apex`() {
        // Capsule: radius equals the smaller half extent.
        val sd = GlassMath.sdRoundedBox(50f, 0f, 50f, 25f, 25f)
        assertThat(sd).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `the field is symmetric in both axes`() {
        val reference = GlassMath.sdRoundedBox(37f, 13f, 50f, 25f, 10f)
        assertThat(GlassMath.sdRoundedBox(-37f, 13f, 50f, 25f, 10f)).isEqualTo(reference)
        assertThat(GlassMath.sdRoundedBox(37f, -13f, 50f, 25f, 10f)).isEqualTo(reference)
        assertThat(GlassMath.sdRoundedBox(-37f, -13f, 50f, 25f, 10f)).isEqualTo(reference)
    }

    @Test
    fun `rounded corners cut the sharp corner off`() {
        // At the corner diagonal a radius shrinks the shape versus a sharp box.
        val sharp = GlassMath.sdRoundedBox(50f, 25f, 50f, 25f, 0f)
        val rounded = GlassMath.sdRoundedBox(50f, 25f, 50f, 25f, 10f)
        assertThat(sharp).isWithin(1e-4f).of(0f)
        assertThat(rounded).isGreaterThan(0f)
    }

    // -- smoothMin ---------------------------------------------------------

    @Test
    fun `zero smoothing collapses to a hard minimum`() {
        assertThat(GlassMath.smoothMin(3f, 7f, 0f)).isWithin(1e-3f).of(3f)
        assertThat(GlassMath.smoothMin(7f, 3f, 0f)).isWithin(1e-3f).of(3f)
    }

    @Test
    fun `smoothMin is symmetric in its operands`() {
        val ab = GlassMath.smoothMin(4f, 9f, 20f)
        val ba = GlassMath.smoothMin(9f, 4f, 20f)
        assertThat(ab).isWithin(1e-4f).of(ba)
    }

    @Test
    fun `smoothMin never exceeds the hard minimum`() {
        for (b in listOf(-10f, 0f, 5f, 40f)) {
            assertThat(GlassMath.smoothMin(5f, b, 25f)).isAtMost(minOf(5f, b) + 1e-4f)
        }
    }

    @Test
    fun `values far apart pass through unchanged`() {
        // This is what makes off-screen sentinel shapes invisible to the union.
        val result = GlassMath.smoothMin(5f, GlassMath.EMPTY_SCENE_DISTANCE, 10f)
        assertThat(result).isWithin(1e-4f).of(5f)
    }

    @Test
    fun `equal values blend below themselves creating the liquid neck`() {
        val k = 30f
        val result = GlassMath.smoothMin(25f, 25f, k)
        assertThat(result).isWithin(1e-4f).of(25f - k * 0.25f)
    }

    // -- sceneSd / liquid merge --------------------------------------------

    private fun twoCircles(separation: Float): Pair<FloatArray, FloatArray> {
        val shapes = listOf(
            PackedGlassShape(0f, 0f, 25f, 25f, 25f),
            PackedGlassShape(separation, 0f, 25f, 25f, 25f),
        )
        return GlassShapePacker.packShapes(shapes) to GlassShapePacker.packCornerRadii(shapes)
    }

    @Test
    fun `without smoothing the gap between two circles stays open`() {
        val (shapes, radii) = twoCircles(separation = 100f)
        val midpoint = GlassMath.sceneSd(50f, 0f, shapes, radii, mergeSmoothing = 0f)
        assertThat(midpoint).isWithin(1e-3f).of(25f)
    }

    @Test
    fun `with enough smoothing two circles melt into one liquid form`() {
        val (shapes, radii) = twoCircles(separation = 100f)
        val midpoint = GlassMath.sceneSd(50f, 0f, shapes, radii, mergeSmoothing = 120f)
        assertThat(midpoint).isLessThan(0f)
    }

    @Test
    fun `a single packed shape matches the raw box field exactly`() {
        val shape = PackedGlassShape(80f, 40f, 60f, 30f, 16f)
        val shapes = GlassShapePacker.packShapes(listOf(shape))
        val radii = GlassShapePacker.packCornerRadii(listOf(shape))
        val scene = GlassMath.sceneSd(100f, 50f, shapes, radii, mergeSmoothing = 24f)
        val direct = GlassMath.sdRoundedBox(100f - 80f, 50f - 40f, 60f, 30f, 16f)
        assertThat(scene).isWithin(1e-3f).of(direct)
    }

    @Test
    fun `sceneSd rejects malformed uniform arrays`() {
        try {
            GlassMath.sceneSd(0f, 0f, FloatArray(3), FloatArray(8), 0f)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessageThat().contains("packed shapes")
        }
    }

    // -- sceneNormal -------------------------------------------------------

    @Test
    fun `the normal near a vertical edge points straight outward`() {
        val shape = PackedGlassShape(0f, 0f, 50f, 25f, 0f)
        val shapes = GlassShapePacker.packShapes(listOf(shape))
        val radii = GlassShapePacker.packCornerRadii(listOf(shape))
        val normal = GlassMath.sceneNormal(45f, 0f, shapes, radii, 0f)
        assertThat(normal[0]).isWithin(1e-3f).of(1f)
        assertThat(normal[1]).isWithin(1e-3f).of(0f)
    }

    @Test
    fun `the normal is always unit length where defined`() {
        val shape = PackedGlassShape(0f, 0f, 50f, 25f, 12f)
        val shapes = GlassShapePacker.packShapes(listOf(shape))
        val radii = GlassShapePacker.packCornerRadii(listOf(shape))
        for ((x, y) in listOf(40f to 10f, -30f to -20f, 55f to 0f, 0f to 30f)) {
            val n = GlassMath.sceneNormal(x, y, shapes, radii, 0f)
            val length = kotlin.math.sqrt(n[0] * n[0] + n[1] * n[1])
            assertThat(length).isWithin(1e-3f).of(1f)
        }
    }

    // -- lensProfile -------------------------------------------------------

    @Test
    fun `the lens bends hardest exactly at the rim`() {
        assertThat(GlassMath.lensProfile(0f, 12f)).isWithin(1e-4f).of(1f)
    }

    @Test
    fun `the lens flattens to nothing at the inner end of the band`() {
        assertThat(GlassMath.lensProfile(12f, 12f)).isEqualTo(0f)
        assertThat(GlassMath.lensProfile(20f, 12f)).isEqualTo(0f)
    }

    @Test
    fun `the lens profile decreases monotonically into the glass`() {
        var previous = Float.MAX_VALUE
        for (step in 0..12) {
            val value = GlassMath.lensProfile(step.toFloat(), 12f)
            assertThat(value).isAtMost(previous)
            previous = value
        }
    }

    @Test
    fun `a zero-height band never refracts`() {
        assertThat(GlassMath.lensProfile(0f, 0f)).isEqualTo(0f)
    }
}

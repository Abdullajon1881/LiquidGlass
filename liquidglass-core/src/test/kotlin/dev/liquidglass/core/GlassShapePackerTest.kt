package dev.liquidglass.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlassShapePackerTest {

    @Test
    fun `real shapes are packed in order as center and half extents`() {
        val packed = GlassShapePacker.packShapes(
            listOf(
                PackedGlassShape(10f, 20f, 30f, 40f, 5f),
                PackedGlassShape(50f, 60f, 70f, 80f, 9f),
            ),
        )
        assertThat(packed.toList().subList(0, 8))
            .containsExactly(10f, 20f, 30f, 40f, 50f, 60f, 70f, 80f)
            .inOrder()
    }

    @Test
    fun `unused slots are parked off-screen with a tiny extent`() {
        val packed = GlassShapePacker.packShapes(listOf(PackedGlassShape(0f, 0f, 10f, 10f, 0f)))
        for (slot in 1 until GlassUniforms.MAX_SHAPES) {
            val base = slot * GlassUniforms.SHAPE_FLOATS
            assertThat(packed[base]).isEqualTo(GlassUniforms.SENTINEL_CENTER)
            assertThat(packed[base + 1]).isEqualTo(GlassUniforms.SENTINEL_CENTER)
            assertThat(packed[base + 2]).isEqualTo(1f)
            assertThat(packed[base + 3]).isEqualTo(1f)
        }
    }

    @Test
    fun `an empty scene packs to all sentinels`() {
        val packed = GlassShapePacker.packShapes(emptyList())
        for (slot in 0 until GlassUniforms.MAX_SHAPES) {
            assertThat(packed[slot * GlassUniforms.SHAPE_FLOATS])
                .isEqualTo(GlassUniforms.SENTINEL_CENTER)
        }
    }

    @Test
    fun `shapes beyond the limit are dropped`() {
        val tooMany = List(12) { PackedGlassShape(it.toFloat(), 0f, 5f, 5f, 0f) }
        val packed = GlassShapePacker.packShapes(tooMany)
        assertThat(packed.size).isEqualTo(GlassUniforms.MAX_SHAPES * GlassUniforms.SHAPE_FLOATS)
        // The last packed slot is shape index 7, not 11.
        val lastBase = (GlassUniforms.MAX_SHAPES - 1) * GlassUniforms.SHAPE_FLOATS
        assertThat(packed[lastBase]).isEqualTo(7f)
    }

    @Test
    fun `corner radii are clamped to the smaller half extent`() {
        val radii = GlassShapePacker.packCornerRadii(
            listOf(
                PackedGlassShape(0f, 0f, 10f, 20f, 100f),
                PackedGlassShape(0f, 0f, 30f, 15f, 8f),
            ),
        )
        assertThat(radii[0]).isEqualTo(10f)
        assertThat(radii[1]).isEqualTo(8f)
    }

    @Test
    fun `unused radius slots are zero`() {
        val radii = GlassShapePacker.packCornerRadii(emptyList())
        assertThat(radii.toList()).containsExactlyElementsIn(List(8) { 0f })
    }

    @Test
    fun `negative shape dimensions are rejected at construction`() {
        try {
            PackedGlassShape(0f, 0f, -1f, 10f, 0f)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessageThat().contains("half extents")
        }
    }

    @Test
    fun `negative corner radius is rejected at construction`() {
        try {
            PackedGlassShape(0f, 0f, 10f, 10f, -2f)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessageThat().contains("Corner radius")
        }
    }
}

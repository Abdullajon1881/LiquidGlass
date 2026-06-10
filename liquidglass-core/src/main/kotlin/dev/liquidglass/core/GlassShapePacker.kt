package dev.liquidglass.core

import kotlin.math.max
import kotlin.math.min

/**
 * A single rounded-rectangle shape in shader-local pixel coordinates, ready to be
 * packed into the scene uniforms. Capsules and circles are rounded rectangles whose
 * corner radius equals the smaller half extent.
 */
public data class PackedGlassShape(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadius: Float,
) {
    init {
        require(halfWidth >= 0f && halfHeight >= 0f) {
            "Shape half extents must be >= 0, got $halfWidth x $halfHeight"
        }
        require(cornerRadius >= 0f) { "Corner radius must be >= 0, got $cornerRadius" }
    }
}

/**
 * Packs glass shapes into the fixed-size uniform arrays the shader expects.
 *
 * Unused slots are parked far off-screen ([GlassUniforms.SENTINEL_CENTER]) with a
 * tiny extent, so the constant-bound shader loop can run over all
 * [GlassUniforms.MAX_SHAPES] slots without branching while empty slots contribute
 * nothing to the smooth union.
 */
public object GlassShapePacker {

    /**
     * Packs up to [GlassUniforms.MAX_SHAPES] shapes as
     * `centerX, centerY, halfWidth, halfHeight` quadruples. Shapes beyond the limit
     * are dropped (callers should treat that as a design smell — Apple's own glass
     * containers stay well under eight elements).
     */
    public fun packShapes(shapes: List<PackedGlassShape>): FloatArray {
        val packed = FloatArray(GlassUniforms.MAX_SHAPES * GlassUniforms.SHAPE_FLOATS)
        for (i in 0 until GlassUniforms.MAX_SHAPES) {
            val base = i * GlassUniforms.SHAPE_FLOATS
            val shape = shapes.getOrNull(i)
            if (shape == null) {
                packed[base] = GlassUniforms.SENTINEL_CENTER
                packed[base + 1] = GlassUniforms.SENTINEL_CENTER
                packed[base + 2] = 1f
                packed[base + 3] = 1f
            } else {
                packed[base] = shape.centerX
                packed[base + 1] = shape.centerY
                packed[base + 2] = shape.halfWidth
                packed[base + 3] = shape.halfHeight
            }
        }
        return packed
    }

    /**
     * Packs corner radii for the same shape list, clamping each radius to the
     * smaller half extent — a radius larger than the shape itself corrupts the SDF.
     * Unused slots get radius `0`.
     */
    public fun packCornerRadii(shapes: List<PackedGlassShape>): FloatArray {
        val packed = FloatArray(GlassUniforms.MAX_SHAPES)
        for (i in 0 until GlassUniforms.MAX_SHAPES) {
            val shape = shapes.getOrNull(i) ?: continue
            val maxRadius = max(min(shape.halfWidth, shape.halfHeight), 0f)
            packed[i] = min(shape.cornerRadius, maxRadius)
        }
        return packed
    }
}

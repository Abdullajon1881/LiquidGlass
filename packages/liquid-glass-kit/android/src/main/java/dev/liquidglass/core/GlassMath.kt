package dev.liquidglass.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure-Kotlin mirror of the signed-distance-field math used by the AGSL shader in
 * [LiquidGlassShaders]. Every function here matches its shader counterpart exactly
 * (same formulas, same epsilon floors), which makes the optics of the glass
 * verifiable with plain JVM unit tests.
 *
 * Conventions: distances are in pixels, negative inside a shape, positive outside.
 */
public object GlassMath {

    /** Epsilon floor shared with the shader to avoid division by zero. */
    public const val EPSILON: Float = 1e-4f

    /** Finite-difference step (px) used for SDF gradients, identical to the shader. */
    public const val GRADIENT_STEP: Float = 1f

    /** SDF value assigned to empty scene slots before any shape is combined. */
    public const val EMPTY_SCENE_DISTANCE: Float = 1_000_000f

    /**
     * Signed distance from point ([px], [py]) to a rounded rectangle centered at the
     * origin with half extents ([halfWidth], [halfHeight]) and corner radius
     * [cornerRadius]. Classic Inigo Quilez formulation.
     */
    public fun sdRoundedBox(
        px: Float,
        py: Float,
        halfWidth: Float,
        halfHeight: Float,
        cornerRadius: Float,
    ): Float {
        val qx = abs(px) - halfWidth + cornerRadius
        val qy = abs(py) - halfHeight + cornerRadius
        val outsideX = max(qx, 0f)
        val outsideY = max(qy, 0f)
        val outside = sqrt(outsideX * outsideX + outsideY * outsideY)
        return min(max(qx, qy), 0f) + outside - cornerRadius
    }

    /**
     * Polynomial smooth minimum of two distances. [smoothing] (px) controls how far
     * apart two surfaces may be while still blending into one liquid form; `0` is a
     * hard union.
     */
    public fun smoothMin(a: Float, b: Float, smoothing: Float): Float {
        val k = max(smoothing, EPSILON)
        val h = (0.5f + 0.5f * (b - a) / k).coerceIn(0f, 1f)
        return lerp(b, a, h) - k * h * (1f - h)
    }

    /**
     * Signed distance of the whole glass scene: the smooth union of every shape in
     * [shapes] (packed as `centerX, centerY, halfWidth, halfHeight` quadruples, see
     * [GlassShapePacker]) with per-shape [cornerRadii].
     */
    public fun sceneSd(
        px: Float,
        py: Float,
        shapes: FloatArray,
        cornerRadii: FloatArray,
        mergeSmoothing: Float,
    ): Float {
        require(shapes.size == GlassUniforms.MAX_SHAPES * GlassUniforms.SHAPE_FLOATS) {
            "shapes must hold ${GlassUniforms.MAX_SHAPES} packed shapes"
        }
        require(cornerRadii.size == GlassUniforms.MAX_SHAPES) {
            "cornerRadii must hold ${GlassUniforms.MAX_SHAPES} entries"
        }
        var d = EMPTY_SCENE_DISTANCE
        for (i in 0 until GlassUniforms.MAX_SHAPES) {
            val base = i * GlassUniforms.SHAPE_FLOATS
            val di = sdRoundedBox(
                px = px - shapes[base],
                py = py - shapes[base + 1],
                halfWidth = shapes[base + 2],
                halfHeight = shapes[base + 3],
                cornerRadius = cornerRadii[i],
            )
            d = smoothMin(d, di, mergeSmoothing)
        }
        return d
    }

    /**
     * Outward-pointing unit normal of the scene SDF at ([px], [py]), computed by
     * central differences with [GRADIENT_STEP], exactly like the shader. Returns a
     * 2-element array `[nx, ny]`; the zero vector for degenerate gradients.
     */
    public fun sceneNormal(
        px: Float,
        py: Float,
        shapes: FloatArray,
        cornerRadii: FloatArray,
        mergeSmoothing: Float,
    ): FloatArray {
        val e = GRADIENT_STEP
        val gx = sceneSd(px + e, py, shapes, cornerRadii, mergeSmoothing) -
            sceneSd(px - e, py, shapes, cornerRadii, mergeSmoothing)
        val gy = sceneSd(px, py + e, shapes, cornerRadii, mergeSmoothing) -
            sceneSd(px, py - e, shapes, cornerRadii, mergeSmoothing)
        val len = sqrt(gx * gx + gy * gy)
        if (len < EPSILON) return floatArrayOf(0f, 0f)
        return floatArrayOf(gx / len, gy / len)
    }

    /**
     * Circular lens profile that drives edge refraction. [edgeDistance] is how far
     * the pixel sits inside the surface (px, >= 0); [refractionHeight] is the width
     * of the refracting band along the rim.
     *
     * Returns `0` at the inner end of the band (flat glass center, no bend) rising
     * to `1` at the rim with an infinite slope — the optical signature of a convex
     * glass edge.
     */
    public fun lensProfile(edgeDistance: Float, refractionHeight: Float): Float {
        if (refractionHeight <= 0f || edgeDistance >= refractionHeight) return 0f
        val x = 1f - edgeDistance / max(refractionHeight, EPSILON)
        return 1f - sqrt(max(1f - x * x, 0f))
    }

    private fun lerp(from: Float, to: Float, fraction: Float): Float =
        from + (to - from) * fraction
}

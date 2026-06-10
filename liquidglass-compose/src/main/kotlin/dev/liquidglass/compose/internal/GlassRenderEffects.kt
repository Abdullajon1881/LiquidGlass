package dev.liquidglass.compose.internal

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import dev.liquidglass.core.GlassUniforms
import dev.liquidglass.core.LiquidGlassShaders
import kotlin.math.abs

private const val MIN_BLUR_RADIUS_PX = 0.05f
private const val SATURATION_EPSILON = 0.01f

/**
 * Fully resolved per-draw scene parameters in glass-layer pixels, mirroring the
 * uniform block of [LiquidGlassShaders.LIQUID_GLASS] one to one.
 */
internal class GlassSceneUniforms(
    val shapes: FloatArray,
    val cornerRadii: FloatArray,
    val mergeSmoothing: Float,
    val refractionHeightPx: Float,
    val refractionAmountPx: Float,
    val chromaticAberration: Float,
    val tintRed: Float,
    val tintGreen: Float,
    val tintBlue: Float,
    val tintAlpha: Float,
    val noiseAlpha: Float,
    val lightDirX: Float,
    val lightDirY: Float,
    val highlightAlpha: Float,
    val highlightWidthPx: Float,
    val pressAmount: Float,
    val pressX: Float,
    val pressY: Float,
)

/**
 * Blur + saturation pre-pass applied to the backdrop before the glass shader
 * samples it (and the whole effect on the BLUR tier). Returns null when both
 * stages are no-ops.
 */
@RequiresApi(31)
internal fun buildBackdropPrepEffect(blurRadiusPx: Float, saturation: Float): RenderEffect? {
    val blur = if (blurRadiusPx > MIN_BLUR_RADIUS_PX) {
        RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
    } else {
        null
    }
    if (abs(saturation - 1f) < SATURATION_EPSILON) return blur
    val filter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) })
    return if (blur != null) {
        RenderEffect.createColorFilterEffect(filter, blur)
    } else {
        RenderEffect.createColorFilterEffect(filter)
    }
}

/**
 * Owns the compiled [RuntimeShader] for one glass node. The AGSL program is
 * compiled once per node; each draw only refreshes uniforms and rebuilds the
 * (cheap) effect chain around it.
 */
@RequiresApi(33)
internal class LiquidGlassRuntimeEffect {

    private val shader = RuntimeShader(LiquidGlassShaders.LIQUID_GLASS)

    fun buildRenderEffect(
        uniforms: GlassSceneUniforms,
        blurRadiusPx: Float,
        saturation: Float,
    ): RenderEffect {
        with(shader) {
            setFloatUniform(GlassUniforms.SHAPES, uniforms.shapes)
            setFloatUniform(GlassUniforms.SHAPE_RADII, uniforms.cornerRadii)
            setFloatUniform(GlassUniforms.MERGE_SMOOTHING, uniforms.mergeSmoothing)
            setFloatUniform(GlassUniforms.REFRACTION_HEIGHT, uniforms.refractionHeightPx)
            setFloatUniform(GlassUniforms.REFRACTION_AMOUNT, uniforms.refractionAmountPx)
            setFloatUniform(GlassUniforms.CHROMATIC_ABERRATION, uniforms.chromaticAberration)
            setFloatUniform(
                GlassUniforms.TINT,
                uniforms.tintRed,
                uniforms.tintGreen,
                uniforms.tintBlue,
                uniforms.tintAlpha,
            )
            setFloatUniform(GlassUniforms.NOISE_ALPHA, uniforms.noiseAlpha)
            setFloatUniform(GlassUniforms.LIGHT_DIRECTION, uniforms.lightDirX, uniforms.lightDirY)
            setFloatUniform(GlassUniforms.HIGHLIGHT_ALPHA, uniforms.highlightAlpha)
            setFloatUniform(GlassUniforms.HIGHLIGHT_WIDTH, uniforms.highlightWidthPx)
            setFloatUniform(GlassUniforms.PRESS_AMOUNT, uniforms.pressAmount)
            setFloatUniform(GlassUniforms.PRESS_POINT, uniforms.pressX, uniforms.pressY)
        }
        val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, GlassUniforms.CONTENT)
        val prep = buildBackdropPrepEffect(blurRadiusPx, saturation) ?: return shaderEffect
        return RenderEffect.createChainEffect(shaderEffect, prep)
    }
}

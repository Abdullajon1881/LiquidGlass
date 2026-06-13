package dev.liquidglass.view.internal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import dev.liquidglass.core.GlassShapePacker
import dev.liquidglass.core.GlassUniforms
import dev.liquidglass.core.LiquidGlassShaders
import dev.liquidglass.core.PackedGlassShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val MIN_BLUR_RADIUS_PX = 0.05f
private const val SATURATION_EPSILON = 0.01f

/** Everything the renderer needs for one frame, resolved to pixels. */
internal class GlassFrame(
    val widthPx: Float,
    val heightPx: Float,
    val cornerRadiusPx: Float,
    val blurRadiusPx: Float,
    val refractionHeightPx: Float,
    val refractionAmountPx: Float,
    val saturation: Float,
    val tintColor: Int,
    val chromaticAberration: Float,
    val noiseAlpha: Float,
    val highlightAlpha: Float,
    val highlightWidthPx: Float,
    val lightAngleDegrees: Float,
    val pressAmount: Float,
    val pressX: Float,
    val pressY: Float,
    val relativeX: Float,
    val relativeY: Float,
)

/**
 * Per-view drawing engine for the View system, mirroring the Compose
 * `GlassPainter` tier behavior with [RenderNode]/[RenderEffect] primitives and
 * the shared AGSL program from `liquidglass-core`.
 */
internal class GlassViewRenderer {

    private var glassNode: RenderNode? = null
    private var runtimeShader: RuntimeShader? = null
    private val clipPath = Path()
    private val scrimPaint = Paint()
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rectF = RectF()

    fun release() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            glassNode?.discardDisplayList()
        }
        glassNode = null
        runtimeShader = null
    }

    /** SHADER (33+) and BLUR (31+) tiers: backdrop slice + RenderEffect. */
    @RequiresApi(31)
    fun drawEffectGlass(
        canvas: Canvas,
        providerNode: RenderNode,
        frame: GlassFrame,
        useShader: Boolean,
    ) {
        val inflate = inflateMargin(frame)
        val layerWidth = (frame.widthPx + inflate * 2f).toInt()
        val layerHeight = (frame.heightPx + inflate * 2f).toInt()
        if (layerWidth <= 0 || layerHeight <= 0) return

        val node = glassNode ?: RenderNode("LiquidGlass").also { glassNode = it }
        node.setPosition(0, 0, layerWidth, layerHeight)
        val recording = node.beginRecording(layerWidth, layerHeight)
        try {
            recording.translate(inflate - frame.relativeX, inflate - frame.relativeY)
            recording.drawRenderNode(providerNode)
        } finally {
            node.endRecording()
        }
        node.setRenderEffect(
            if (useShader && android.os.Build.VERSION.SDK_INT >= 33) {
                buildShaderEffect(frame, inflate)
            } else {
                buildBackdropPrepEffect(frame.blurRadiusPx, frame.saturation)
            },
        )

        canvas.save()
        if (!useShader) {
            canvas.clipPath(shapePath(frame))
        }
        canvas.translate(-inflate, -inflate)
        canvas.drawRenderNode(node)
        canvas.restore()

        if (!useShader) {
            if (Color.alpha(frame.tintColor) > 0) {
                drawFill(canvas, frame, frame.tintColor)
            }
            drawRim(canvas, frame)
        }
    }

    /** API 29–30: real backdrop, clipped, with a scrim — no RenderEffect. */
    @RequiresApi(29)
    fun drawBackdropScrim(
        canvas: Canvas,
        providerNode: RenderNode,
        frame: GlassFrame,
        scrimColor: Int,
    ) {
        canvas.save()
        canvas.clipPath(shapePath(frame))
        canvas.translate(-frame.relativeX, -frame.relativeY)
        canvas.drawRenderNode(providerNode)
        canvas.restore()
        drawFill(canvas, frame, scrimColor)
        drawRim(canvas, frame)
    }

    /** Last resort everywhere: translucent scrim + rim, no backdrop sample. */
    fun drawPlainScrim(canvas: Canvas, frame: GlassFrame, scrimColor: Int) {
        drawFill(canvas, frame, scrimColor)
        drawRim(canvas, frame)
    }

    @RequiresApi(33)
    private fun buildShaderEffect(frame: GlassFrame, inflate: Float): RenderEffect {
        val shader = runtimeShader
            ?: RuntimeShader(LiquidGlassShaders.LIQUID_GLASS).also { runtimeShader = it }
        val shape = PackedGlassShape(
            centerX = frame.widthPx / 2f + inflate,
            centerY = frame.heightPx / 2f + inflate,
            halfWidth = frame.widthPx / 2f,
            halfHeight = frame.heightPx / 2f,
            cornerRadius = frame.cornerRadiusPx,
        )
        val shapes = listOf(shape)
        val angleRadians = Math.toRadians(frame.lightAngleDegrees.toDouble())
        with(shader) {
            setFloatUniform(GlassUniforms.SHAPES, GlassShapePacker.packShapes(shapes))
            setFloatUniform(GlassUniforms.SHAPE_RADII, GlassShapePacker.packCornerRadii(shapes))
            setFloatUniform(GlassUniforms.MERGE_SMOOTHING, 0f)
            setFloatUniform(GlassUniforms.REFRACTION_HEIGHT, frame.refractionHeightPx)
            setFloatUniform(GlassUniforms.REFRACTION_AMOUNT, frame.refractionAmountPx)
            setFloatUniform(GlassUniforms.CHROMATIC_ABERRATION, frame.chromaticAberration)
            setFloatUniform(
                GlassUniforms.TINT,
                Color.red(frame.tintColor) / 255f,
                Color.green(frame.tintColor) / 255f,
                Color.blue(frame.tintColor) / 255f,
                Color.alpha(frame.tintColor) / 255f,
            )
            setFloatUniform(GlassUniforms.NOISE_ALPHA, frame.noiseAlpha)
            setFloatUniform(
                GlassUniforms.LIGHT_DIRECTION,
                cos(angleRadians).toFloat(),
                sin(angleRadians).toFloat(),
            )
            setFloatUniform(GlassUniforms.HIGHLIGHT_ALPHA, frame.highlightAlpha)
            setFloatUniform(GlassUniforms.HIGHLIGHT_WIDTH, frame.highlightWidthPx)
            setFloatUniform(GlassUniforms.PRESS_AMOUNT, max(frame.pressAmount, 0f))
            setFloatUniform(
                GlassUniforms.PRESS_POINT,
                frame.pressX + inflate,
                frame.pressY + inflate,
            )
        }
        val shaderEffect =
            RenderEffect.createRuntimeShaderEffect(shader, GlassUniforms.CONTENT)
        val prep = buildBackdropPrepEffect(frame.blurRadiusPx, frame.saturation)
            ?: return shaderEffect
        return RenderEffect.createChainEffect(shaderEffect, prep)
    }

    @RequiresApi(31)
    private fun buildBackdropPrepEffect(blurRadiusPx: Float, saturation: Float): RenderEffect? {
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

    private fun inflateMargin(frame: GlassFrame): Float =
        frame.blurRadiusPx * 2f + abs(frame.refractionAmountPx) + 4f

    private fun shapePath(frame: GlassFrame): Path {
        clipPath.rewind()
        rectF.set(0f, 0f, frame.widthPx, frame.heightPx)
        clipPath.addRoundRect(rectF, frame.cornerRadiusPx, frame.cornerRadiusPx, Path.Direction.CW)
        return clipPath
    }

    private fun drawFill(canvas: Canvas, frame: GlassFrame, color: Int) {
        scrimPaint.shader = null
        scrimPaint.color = color
        rectF.set(0f, 0f, frame.widthPx, frame.heightPx)
        canvas.drawRoundRect(rectF, frame.cornerRadiusPx, frame.cornerRadiusPx, scrimPaint)
    }

    private fun drawRim(canvas: Canvas, frame: GlassFrame) {
        if (frame.highlightAlpha <= 0f || frame.highlightWidthPx <= 0f) return
        val angleRadians = Math.toRadians(frame.lightAngleDegrees.toDouble())
        val dirX = cos(angleRadians).toFloat()
        val dirY = sin(angleRadians).toFloat()
        val cx = frame.widthPx / 2f
        val cy = frame.heightPx / 2f
        val reach = max(frame.widthPx, frame.heightPx) / 2f
        val alpha = (frame.highlightAlpha * 255).toInt().coerceIn(0, 255)
        rimPaint.strokeWidth = frame.highlightWidthPx
        rimPaint.shader = LinearGradient(
            cx + dirX * reach, cy + dirY * reach,
            cx - dirX * reach, cy - dirY * reach,
            intArrayOf(
                Color.argb(alpha, 255, 255, 255),
                Color.argb((alpha * 0.08f).toInt(), 255, 255, 255),
                Color.argb((alpha * 0.55f).toInt(), 255, 255, 255),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        rectF.set(0f, 0f, frame.widthPx, frame.heightPx)
        canvas.drawRoundRect(rectF, frame.cornerRadiusPx, frame.cornerRadiusPx, rimPaint)
    }
}

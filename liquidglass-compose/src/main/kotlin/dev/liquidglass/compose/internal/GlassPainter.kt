package dev.liquidglass.compose.internal

import android.os.Build
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.IntSize
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.core.GlassRenderTier
import dev.liquidglass.core.GlassShapePacker
import dev.liquidglass.core.PackedGlassShape
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val DEFAULT_SCRIM_ALPHA = 0.35f
private const val LAYER_PADDING_PX = 4f

/**
 * Draws one glass scene (one or more shapes over a shared backdrop) for a node.
 *
 * The painter records the relevant slice of the provider's backdrop into the
 * node's own [GraphicsLayer] — inflated by enough margin that blur kernels and
 * outward refraction never sample clamped edge pixels — then applies the
 * tier-appropriate treatment:
 *
 *  - [GlassRenderTier.SHADER]: the full AGSL pipeline; the shader's SDF mask
 *    doubles as an antialiased clip, so no geometric clipping is needed.
 *  - [GlassRenderTier.BLUR]: blur + saturation `RenderEffect`, geometric clip
 *    to the shape union, optional tint wash, drawn rim highlight.
 *  - [GlassRenderTier.SCRIM]: unblurred backdrop, geometric clip, translucent
 *    scrim, drawn rim highlight.
 */
internal class GlassPainter {

    private var runtimeEffect: LiquidGlassRuntimeEffect? = null

    fun drawScene(
        scope: DrawScope,
        tier: GlassRenderTier,
        providerLayer: GraphicsLayer,
        positionInProvider: Offset,
        glassLayer: GraphicsLayer,
        shapes: List<PackedGlassShape>,
        mergeSmoothingPx: Float,
        style: GlassStyle,
        pressAmount: Float,
        pressPoint: Offset,
    ): Unit = with(scope) {
        if (shapes.isEmpty()) return
        val blurPx = style.blurRadius.toPx()
        val refractionAmountPx = style.refraction.amount.toPx()
        val inflate =
            ceil(blurPx * 2f + abs(refractionAmountPx) + mergeSmoothingPx * 0.5f) + LAYER_PADDING_PX
        val layerSize = IntSize(
            ceil(size.width + inflate * 2f).toInt(),
            ceil(size.height + inflate * 2f).toInt(),
        )
        if (layerSize.width <= 0 || layerSize.height <= 0) return

        glassLayer.record(
            density = this,
            layoutDirection = layoutDirection,
            size = layerSize,
        ) {
            translate(inflate - positionInProvider.x, inflate - positionInProvider.y) {
                drawLayer(providerLayer)
            }
        }

        when (tier) {
            GlassRenderTier.SHADER ->
                drawShaderTier(
                    glassLayer, shapes, inflate, mergeSmoothingPx, style, blurPx,
                    refractionAmountPx, pressAmount, pressPoint,
                )
            GlassRenderTier.BLUR ->
                drawBlurTier(glassLayer, shapes, inflate, style, blurPx)
            GlassRenderTier.SCRIM ->
                drawScrimTier(glassLayer, shapes, inflate, style)
        }
    }

    private fun DrawScope.drawShaderTier(
        glassLayer: GraphicsLayer,
        shapes: List<PackedGlassShape>,
        inflate: Float,
        mergeSmoothingPx: Float,
        style: GlassStyle,
        blurPx: Float,
        refractionAmountPx: Float,
        pressAmount: Float,
        pressPoint: Offset,
    ) {
        if (Build.VERSION.SDK_INT < 33) return
        val effect = runtimeEffect ?: LiquidGlassRuntimeEffect().also { runtimeEffect = it }
        val uniforms = buildUniforms(
            shapes, inflate, mergeSmoothingPx, style, refractionAmountPx,
            pressAmount, pressPoint,
        )
        glassLayer.renderEffect = effect
            .buildRenderEffect(uniforms, blurPx, style.saturation)
            .asComposeRenderEffect()
        translate(-inflate, -inflate) {
            drawLayer(glassLayer)
        }
    }

    private fun DrawScope.drawBlurTier(
        glassLayer: GraphicsLayer,
        shapes: List<PackedGlassShape>,
        inflate: Float,
        style: GlassStyle,
        blurPx: Float,
    ) {
        if (Build.VERSION.SDK_INT < 31) return
        glassLayer.renderEffect =
            buildBackdropPrepEffect(blurPx, style.saturation)?.asComposeRenderEffect()
        clipPath(buildUnionPath(shapes)) {
            translate(-inflate, -inflate) {
                drawLayer(glassLayer)
            }
            if (style.tint.isSpecified && style.tint.alpha > 0f) {
                drawRect(style.tint)
            }
        }
        drawRimHighlights(shapes, style)
    }

    private fun DrawScope.drawScrimTier(
        glassLayer: GraphicsLayer,
        shapes: List<PackedGlassShape>,
        inflate: Float,
        style: GlassStyle,
    ) {
        glassLayer.renderEffect = null
        val scrim = style.fallbackScrim.takeOrElse {
            if (style.tint.isSpecified && style.tint.alpha > 0f) {
                style.tint.copy(alpha = max(style.tint.alpha, DEFAULT_SCRIM_ALPHA))
            } else {
                Color.White.copy(alpha = DEFAULT_SCRIM_ALPHA)
            }
        }
        clipPath(buildUnionPath(shapes)) {
            translate(-inflate, -inflate) {
                drawLayer(glassLayer)
            }
            drawRect(scrim)
        }
        drawRimHighlights(shapes, style)
    }

    private fun DrawScope.buildUniforms(
        shapes: List<PackedGlassShape>,
        inflate: Float,
        mergeSmoothingPx: Float,
        style: GlassStyle,
        refractionAmountPx: Float,
        pressAmount: Float,
        pressPoint: Offset,
    ): GlassSceneUniforms {
        val inflated = shapes.map {
            it.copy(centerX = it.centerX + inflate, centerY = it.centerY + inflate)
        }
        val tint = if (style.tint.isSpecified) style.tint.convert(ColorSpaces.Srgb) else null
        val angleRadians = Math.toRadians(style.highlight.lightAngleDegrees.toDouble())
        return GlassSceneUniforms(
            shapes = GlassShapePacker.packShapes(inflated),
            cornerRadii = GlassShapePacker.packCornerRadii(inflated),
            mergeSmoothing = mergeSmoothingPx,
            refractionHeightPx = style.refraction.height.toPx(),
            refractionAmountPx = refractionAmountPx,
            chromaticAberration = style.chromaticAberration,
            tintRed = tint?.red ?: 0f,
            tintGreen = tint?.green ?: 0f,
            tintBlue = tint?.blue ?: 0f,
            tintAlpha = tint?.alpha ?: 0f,
            noiseAlpha = style.noiseAlpha,
            lightDirX = cos(angleRadians).toFloat(),
            lightDirY = sin(angleRadians).toFloat(),
            highlightAlpha = style.highlight.alpha,
            highlightWidthPx = style.highlight.width.toPx(),
            pressAmount = pressAmount,
            pressX = pressPoint.x + inflate,
            pressY = pressPoint.y + inflate,
        )
    }

    private fun buildUnionPath(shapes: List<PackedGlassShape>): Path {
        val path = Path()
        for (shape in shapes) {
            path.addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset(
                            shape.centerX - shape.halfWidth,
                            shape.centerY - shape.halfHeight,
                        ),
                        size = Size(shape.halfWidth * 2f, shape.halfHeight * 2f),
                    ),
                    cornerRadius = CornerRadius(shape.cornerRadius),
                ),
            )
        }
        return path
    }

    private fun DrawScope.drawRimHighlights(
        shapes: List<PackedGlassShape>,
        style: GlassStyle,
    ) {
        val highlight = style.highlight
        if (highlight.alpha <= 0f || highlight.width.value <= 0f) return
        val strokeWidth = highlight.width.toPx()
        val angleRadians = Math.toRadians(highlight.lightAngleDegrees.toDouble())
        val direction = Offset(cos(angleRadians).toFloat(), sin(angleRadians).toFloat())
        for (shape in shapes) {
            val center = Offset(shape.centerX, shape.centerY)
            val reach = max(shape.halfWidth, shape.halfHeight)
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = highlight.alpha),
                    Color.White.copy(alpha = highlight.alpha * 0.08f),
                    Color.White.copy(alpha = highlight.alpha * 0.55f),
                ),
                start = center + direction * reach,
                end = center - direction * reach,
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(
                    shape.centerX - shape.halfWidth,
                    shape.centerY - shape.halfHeight,
                ),
                size = Size(shape.halfWidth * 2f, shape.halfHeight * 2f),
                cornerRadius = CornerRadius(shape.cornerRadius),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

package dev.liquidglass.compose.container

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.LocalLiquidGlassTier
import dev.liquidglass.compose.internal.GlassMotion
import dev.liquidglass.compose.internal.GlassPainter
import dev.liquidglass.compose.resolveCornerRadiusPx
import dev.liquidglass.core.GlassRenderTier
import dev.liquidglass.core.PackedGlassShape

/**
 * Renders one shared liquid surface for every [glassEffect] child registered
 * with [state] — the Android counterpart of the iOS `GlassEffectContainer`.
 *
 * Children marked with `Modifier.glassEffect(state, ...)`, at any nesting depth
 * inside this element, are drawn as a single smooth-min union of shapes: shapes
 * closer than [spacing] melt into one another, and children entering or leaving
 * the composition morph out of and back into their neighbors. Animate child
 * positions normally — the liquid merging follows for free.
 *
 * A container supports up to 8 simultaneous glass children (extra shapes are
 * dropped); Apple's own guidance keeps glass clusters far smaller.
 *
 * @param state the container state from [rememberLiquidGlassContainerState].
 * @param style the appearance of the shared surface; per-child silhouette
 *   overrides come from [glassEffect].
 * @param spacing the merge distance: shapes closer than this melt together.
 */
public fun Modifier.liquidGlassContainer(
    state: LiquidGlassContainerState,
    style: GlassStyle = GlassStyle.Regular,
    spacing: Dp = 16.dp,
): Modifier = this then LiquidGlassContainerElement(state, style, spacing)

/**
 * Convenience [Box] wrapper around [liquidGlassContainer] for the common case.
 *
 * ```
 * val container = rememberLiquidGlassContainerState(glassState)
 * LiquidGlassContainer(container, spacing = 40.dp) {
 *     Column {
 *         Box(Modifier.size(56.dp).glassEffect(container, id = "a")) { ... }
 *         Box(Modifier.size(56.dp).glassEffect(container, id = "b")) { ... }
 *     }
 * }
 * ```
 */
@Composable
public fun LiquidGlassContainer(
    state: LiquidGlassContainerState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Regular,
    spacing: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.liquidGlassContainer(state, style, spacing), content = content)
}

private class LiquidGlassContainerElement(
    private val state: LiquidGlassContainerState,
    private val style: GlassStyle,
    private val spacing: Dp,
) : ModifierNodeElement<LiquidGlassContainerNode>() {

    override fun create(): LiquidGlassContainerNode =
        LiquidGlassContainerNode(state, style, spacing)

    override fun update(node: LiquidGlassContainerNode) {
        node.update(state, style, spacing)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlassContainer"
        properties["style"] = style
        properties["spacing"] = spacing
    }

    override fun equals(other: Any?): Boolean = other is LiquidGlassContainerElement &&
        other.state === state && other.style == style && other.spacing == spacing

    override fun hashCode(): Int {
        var result = System.identityHashCode(state)
        result = 31 * result + style.hashCode()
        result = 31 * result + spacing.hashCode()
        return result
    }
}

private class LiquidGlassContainerNode(
    private var state: LiquidGlassContainerState,
    private var style: GlassStyle,
    private var spacing: Dp,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private val painter = GlassPainter()
    private var glassLayer: GraphicsLayer? = null
    private var lastPosition: Offset = Offset.Unspecified

    fun update(newState: LiquidGlassContainerState, newStyle: GlassStyle, newSpacing: Dp) {
        if (state !== newState || style != newStyle || spacing != newSpacing) {
            state = newState
            style = newStyle
            spacing = newSpacing
            invalidateDraw()
        }
    }

    override fun onAttach() {
        glassLayer = currentValueOf(LocalGraphicsContext).createGraphicsLayer()
    }

    override fun onDetach() {
        glassLayer?.let { currentValueOf(LocalGraphicsContext).releaseGraphicsLayer(it) }
        glassLayer = null
        state.coordinates = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        state.coordinates = coordinates
        val position = positionInProvider()
        if (position != lastPosition) {
            lastPosition = position
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        if (size.width < 1f || size.height < 1f) {
            drawContent()
            return
        }
        state.providerState.positionTick
        val providerLayer = state.providerState.contentLayer
        val layer = glassLayer
        val position = positionInProvider()
        val shapes = collectShapes()

        if (providerLayer != null && layer != null && position.isSpecified && shapes.isNotEmpty()) {
            painter.drawScene(
                scope = this,
                tier = resolveTier(),
                providerLayer = providerLayer,
                positionInProvider = position,
                glassLayer = layer,
                shapes = shapes,
                mergeSmoothingPx = spacing.toPx(),
                style = style,
                pressAmount = state.pressProgress.value,
                pressPoint = state.pressPoint,
            )
        }
        drawContent()
    }

    private fun ContentDrawScope.collectShapes(): List<PackedGlassShape> =
        state.shapes.values.mapNotNull { shape ->
            val rect = shape.targetRect
            if (rect.width < 1f || rect.height < 1f) return@mapNotNull null
            val progress = shape.progress.value
            if (progress <= 0.01f) return@mapNotNull null
            val morphScale =
                GlassMotion.MORPH_MIN_SCALE + (1f - GlassMotion.MORPH_MIN_SCALE) * progress
            val silhouette = shape.shapeOverride ?: style.shape
            val cornerRadius = silhouette.resolveCornerRadiusPx(rect.size, this)
            PackedGlassShape(
                centerX = rect.center.x,
                centerY = rect.center.y,
                halfWidth = rect.width / 2f * morphScale,
                halfHeight = rect.height / 2f * morphScale,
                cornerRadius = cornerRadius * morphScale,
            )
        }

    private fun resolveTier(): GlassRenderTier {
        val local = currentValueOf(LocalLiquidGlassTier)
        val fromState = state.providerState.requestedTier
        val requested = when {
            local == null -> fromState
            fromState == null -> local
            local.ordinal >= fromState.ordinal -> local
            else -> fromState
        }
        return GlassRenderTier.select(Build.VERSION.SDK_INT, requested)
    }

    private fun positionInProvider(): Offset {
        val provider = state.providerState.coordinates ?: return Offset.Unspecified
        val container = state.coordinates ?: return Offset.Unspecified
        if (!provider.isAttached || !container.isAttached) return Offset.Unspecified
        return provider.localPositionOf(container, Offset.Zero)
    }
}

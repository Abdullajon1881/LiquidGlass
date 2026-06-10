package dev.liquidglass.compose

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.IntSize
import dev.liquidglass.compose.internal.GlassMotion
import dev.liquidglass.compose.internal.GlassPainter
import dev.liquidglass.core.GlassRenderTier
import dev.liquidglass.core.PackedGlassShape
import kotlinx.coroutines.launch

/**
 * Renders this element as Liquid Glass over the backdrop registered with
 * [liquidGlassProvider] for the same [state].
 *
 * The element's own content (text, icons) draws on top of the glass, unclipped
 * and untouched. The glass paints behind it: the backdrop is blurred, saturated,
 * refracted along the rim, tinted, lit and grained according to [style].
 *
 * When [GlassStyle.isInteractive] is set the element answers touch like gel —
 * it springs slightly larger, the lens bulges around the finger, and the rim
 * brightens; release lets it bounce back.
 *
 * The element must be drawn after (on top of) the provider. For several glass
 * elements that should melt together when they approach, use
 * [LiquidGlassContainer] instead of stacking standalone modifiers.
 *
 * @param state the provider state shared with [liquidGlassProvider].
 * @param style the appearance, see [GlassStyle]; defaults to [GlassStyle.Regular].
 */
public fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: GlassStyle = GlassStyle.Regular,
): Modifier = this then LiquidGlassElement(state, style)

private class LiquidGlassElement(
    private val state: LiquidGlassProviderState,
    private val style: GlassStyle,
) : ModifierNodeElement<LiquidGlassNode>() {

    override fun create(): LiquidGlassNode = LiquidGlassNode(state, style)

    override fun update(node: LiquidGlassNode) {
        node.update(state, style)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["state"] = state
        properties["style"] = style
    }

    override fun equals(other: Any?): Boolean = other is LiquidGlassElement &&
        other.state === state && other.style == style

    override fun hashCode(): Int = 31 * System.identityHashCode(state) + style.hashCode()
}

private class LiquidGlassNode(
    private var state: LiquidGlassProviderState,
    private var style: GlassStyle,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    private val painter = GlassPainter()
    private var glassLayer: GraphicsLayer? = null
    private var consumerCoordinates: LayoutCoordinates? = null
    private var lastPosition: Offset = Offset.Unspecified
    private val pressProgress = Animatable(0f)
    private var pressPoint = Offset.Zero

    fun update(newState: LiquidGlassProviderState, newStyle: GlassStyle) {
        var changed = false
        if (state !== newState) {
            state = newState
            changed = true
        }
        if (style != newStyle) {
            style = newStyle
            changed = true
        }
        if (!style.isInteractive && pressProgress.targetValue != 0f) {
            releasePress()
        }
        if (changed) invalidateDraw()
    }

    override fun onAttach() {
        glassLayer = currentValueOf(LocalGraphicsContext).createGraphicsLayer()
    }

    override fun onDetach() {
        glassLayer?.let { currentValueOf(LocalGraphicsContext).releaseGraphicsLayer(it) }
        glassLayer = null
        consumerCoordinates = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        consumerCoordinates = coordinates
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
        // Snapshot reads that re-invalidate this draw when the provider attaches,
        // re-records, or moves, and while the press animation runs.
        state.positionTick
        val providerLayer = state.contentLayer
        val layer = glassLayer
        val position = positionInProvider()
        val press = pressProgress.value

        val drawGlassAndContent: ContentDrawScope.() -> Unit = {
            if (providerLayer != null && layer != null && position.isSpecified) {
                val cornerRadius = style.shape.resolveCornerRadiusPx(size, this)
                painter.drawScene(
                    scope = this,
                    tier = resolveTier(),
                    providerLayer = providerLayer,
                    positionInProvider = position,
                    glassLayer = layer,
                    shapes = listOf(
                        PackedGlassShape(
                            centerX = size.width / 2f,
                            centerY = size.height / 2f,
                            halfWidth = size.width / 2f,
                            halfHeight = size.height / 2f,
                            cornerRadius = cornerRadius,
                        ),
                    ),
                    mergeSmoothingPx = 0f,
                    style = style,
                    pressAmount = press,
                    pressPoint = pressPoint,
                )
            }
            drawContent()
        }

        if (press > 0f) {
            scale(scale = 1f + GlassMotion.PRESS_SCALE_MAX * press) {
                this@draw.drawGlassAndContent()
            }
        } else {
            this.drawGlassAndContent()
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (!style.isInteractive || pass != PointerEventPass.Initial) return
        when (pointerEvent.type) {
            PointerEventType.Press -> {
                pointerEvent.changes.firstOrNull()?.let { pressPoint = it.position }
                coroutineScope.launch { pressProgress.animateTo(1f, GlassMotion.PressIn) }
            }
            PointerEventType.Release -> {
                if (pointerEvent.changes.none { it.pressed }) releasePress()
            }
        }
    }

    override fun onCancelPointerInput() {
        if (style.isInteractive) releasePress()
    }

    private fun releasePress() {
        coroutineScope.launch { pressProgress.animateTo(0f, GlassMotion.Release) }
    }

    private fun resolveTier(): GlassRenderTier {
        val local = currentValueOf(LocalLiquidGlassTier)
        val fromState = state.requestedTier
        val requested = when {
            local == null -> fromState
            fromState == null -> local
            // The lower fidelity (higher ordinal) wins.
            local.ordinal >= fromState.ordinal -> local
            else -> fromState
        }
        return GlassRenderTier.select(Build.VERSION.SDK_INT, requested)
    }

    private fun positionInProvider(): Offset {
        val provider = state.coordinates ?: return Offset.Unspecified
        val consumer = consumerCoordinates ?: return Offset.Unspecified
        if (!provider.isAttached || !consumer.isAttached) return Offset.Unspecified
        return provider.localPositionOf(consumer, Offset.Zero)
    }
}

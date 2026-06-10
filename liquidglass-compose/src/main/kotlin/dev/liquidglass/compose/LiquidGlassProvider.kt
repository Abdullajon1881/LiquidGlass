package dev.liquidglass.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

/**
 * Marks this element's content as the backdrop seen through Liquid Glass.
 *
 * The content is recorded into a reusable [GraphicsLayer] and drawn normally, so
 * the visual output of this element does not change. Glass elements created with
 * [liquidGlass] or [LiquidGlassContainer] that share [state] sample the recording
 * to build their refraction.
 *
 * One state belongs to exactly one provider at a time.
 */
public fun Modifier.liquidGlassProvider(state: LiquidGlassProviderState): Modifier =
    this then LiquidGlassProviderElement(state)

private class LiquidGlassProviderElement(
    private val state: LiquidGlassProviderState,
) : ModifierNodeElement<LiquidGlassProviderNode>() {

    override fun create(): LiquidGlassProviderNode = LiquidGlassProviderNode(state)

    override fun update(node: LiquidGlassProviderNode) {
        node.updateState(state)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlassProvider"
        properties["state"] = state
    }

    override fun equals(other: Any?): Boolean =
        other is LiquidGlassProviderElement && other.state === state

    override fun hashCode(): Int = System.identityHashCode(state)
}

private class LiquidGlassProviderNode(
    private var state: LiquidGlassProviderState,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private var layer: GraphicsLayer? = null

    fun updateState(newState: LiquidGlassProviderState) {
        if (newState === state) return
        state.onProviderDetached()
        state = newState
        attachToState()
    }

    override fun onAttach() {
        layer = currentValueOf(LocalGraphicsContext).createGraphicsLayer()
        attachToState()
    }

    override fun onDetach() {
        state.onProviderDetached()
        layer?.let { currentValueOf(LocalGraphicsContext).releaseGraphicsLayer(it) }
        layer = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        state.onProviderPositioned(coordinates)
    }

    override fun ContentDrawScope.draw() {
        val contentLayer = layer
        if (contentLayer == null || size.width < 1f || size.height < 1f) {
            drawContent()
            return
        }
        contentLayer.record(
            density = this,
            layoutDirection = layoutDirection,
            size = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
        ) {
            this@draw.drawContent()
        }
        drawLayer(contentLayer)
    }

    private fun attachToState() {
        check(state.contentLayer == null || state.contentLayer === layer) {
            "LiquidGlassProviderState is already attached to another liquidGlassProvider. " +
                "Each state may back exactly one provider."
        }
        state.contentLayer = layer
    }
}

package dev.liquidglass.compose.container

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import dev.liquidglass.compose.GlassShape

/**
 * Registers this element as a glass shape on the shared liquid surface of the
 * [liquidGlassContainer] holding the same [state].
 *
 * The element draws only its content; the glass itself is painted once by the
 * container, which is what lets nearby shapes melt together and lets shapes
 * morph in and out as children enter and leave the composition. Works at any
 * nesting depth inside the container.
 *
 * @param state the shared container state from [rememberLiquidGlassContainerState].
 * @param id stable identity for morphing across structural changes. Children
 *   that may leave and re-enter the composition should pass one; `null` uses
 *   the node identity.
 * @param shape silhouette override for this child; `null` inherits the
 *   container style's shape.
 * @param interactive when true, touching this child presses the shared surface
 *   (gel bulge + brighter rim) around the touch point.
 */
public fun Modifier.glassEffect(
    state: LiquidGlassContainerState,
    id: Any? = null,
    shape: GlassShape? = null,
    interactive: Boolean = false,
): Modifier = this then GlassEffectChildElement(state, id, shape, interactive)

/**
 * Modifier element behind [glassEffect]: registers the child's geometry with
 * the container and routes presses to the shared surface.
 */
internal class GlassEffectChildElement(
    private val state: LiquidGlassContainerState,
    private val id: Any?,
    private val shape: GlassShape?,
    private val interactive: Boolean,
) : ModifierNodeElement<GlassEffectChildNode>() {

    override fun create(): GlassEffectChildNode =
        GlassEffectChildNode(state, id, shape, interactive)

    override fun update(node: GlassEffectChildNode) {
        node.update(state, id, shape, interactive)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassEffect"
        properties["id"] = id
        properties["shape"] = shape
        properties["interactive"] = interactive
    }

    override fun equals(other: Any?): Boolean = other is GlassEffectChildElement &&
        other.state === state && other.id == id && other.shape == shape &&
        other.interactive == interactive

    override fun hashCode(): Int {
        var result = System.identityHashCode(state)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (shape?.hashCode() ?: 0)
        result = 31 * result + interactive.hashCode()
        return result
    }
}

internal class GlassEffectChildNode(
    private var state: LiquidGlassContainerState,
    private var id: Any?,
    private var shape: GlassShape?,
    private var interactive: Boolean,
) : Modifier.Node(),
    GlobalPositionAwareModifierNode,
    PointerInputModifierNode {

    private var registeredKey: Any? = null
    private var registeredShape: ContainerGlassShape? = null
    private var rectInContainer: Rect = Rect.Zero

    fun update(
        newState: LiquidGlassContainerState,
        newId: Any?,
        newShape: GlassShape?,
        newInteractive: Boolean,
    ) {
        val keyChanged = newState !== state || resolveKey(newId) != registeredKey
        state = newState
        id = newId
        shape = newShape
        interactive = newInteractive
        if (isAttached && keyChanged) {
            unregister()
            register()
        }
        registeredShape?.shapeOverride = newShape
    }

    override fun onAttach() {
        register()
    }

    override fun onDetach() {
        unregister()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val container = state.coordinates ?: return
        if (!container.isAttached || !coordinates.isAttached) return
        val topLeft = container.localPositionOf(coordinates, Offset.Zero)
        rectInContainer = Rect(topLeft, coordinates.size.toSize())
        registeredShape?.targetRect = rectInContainer
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (!interactive || pass != PointerEventPass.Initial) return
        when (pointerEvent.type) {
            PointerEventType.Press -> {
                val local = pointerEvent.changes.firstOrNull()?.position ?: return
                state.press(rectInContainer.topLeft + local)
            }
            PointerEventType.Release -> {
                if (pointerEvent.changes.none { it.pressed }) state.release()
            }
        }
    }

    override fun onCancelPointerInput() {
        if (interactive) state.release()
    }

    private fun register() {
        val key = resolveKey(id)
        registeredKey = key
        registeredShape = state.registerShape(key).also {
            it.shapeOverride = shape
            if (rectInContainer != Rect.Zero) it.targetRect = rectInContainer
        }
    }

    private fun unregister() {
        registeredKey?.let { state.unregisterShape(it) }
        registeredKey = null
        registeredShape = null
    }

    private fun resolveKey(id: Any?): Any = id ?: this
}

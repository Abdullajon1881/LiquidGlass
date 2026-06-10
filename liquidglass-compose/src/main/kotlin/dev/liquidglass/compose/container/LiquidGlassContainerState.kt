package dev.liquidglass.compose.container

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import dev.liquidglass.compose.GlassShape
import dev.liquidglass.compose.LiquidGlassProviderState
import dev.liquidglass.compose.internal.GlassMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One glass shape registered by a [glassEffect] child of a liquid glass
 * container.
 *
 * [progress] drives the morph: shapes scale up from [GlassMotion.MORPH_MIN_SCALE]
 * when they enter the composition and shrink back before being released, which —
 * combined with the smooth-min union in the shader — makes them visually emerge
 * from and dissolve into their neighbors.
 */
internal class ContainerGlassShape {
    var targetRect: Rect by mutableStateOf(Rect.Zero)
    var shapeOverride: GlassShape? by mutableStateOf(null)
    val progress: Animatable<Float, *> = Animatable(0f)
    var isRemoving: Boolean = false
}

/**
 * Connects a [liquidGlassContainer] surface with its [glassEffect] children:
 * the live set of child shapes, the container's coordinates, and the shared
 * press state. Create one with [rememberLiquidGlassContainerState] and pass the
 * same instance to the container and to every child.
 */
@Stable
public class LiquidGlassContainerState internal constructor(
    internal val providerState: LiquidGlassProviderState,
    private val animationScope: CoroutineScope,
) {
    internal val shapes = mutableStateMapOf<Any, ContainerGlassShape>()

    internal var coordinates: LayoutCoordinates? = null

    internal var pressPoint: Offset by mutableStateOf(Offset.Zero)

    internal val pressProgress: Animatable<Float, *> = Animatable(0f)

    internal fun registerShape(key: Any): ContainerGlassShape {
        val existing = shapes[key]
        if (existing != null) {
            existing.isRemoving = false
            animationScope.launch { existing.progress.animateTo(1f, GlassMotion.Appear) }
            return existing
        }
        val shape = ContainerGlassShape()
        shapes[key] = shape
        animationScope.launch { shape.progress.animateTo(1f, GlassMotion.Appear) }
        return shape
    }

    internal fun unregisterShape(key: Any) {
        val shape = shapes[key] ?: return
        shape.isRemoving = true
        animationScope.launch {
            shape.progress.animateTo(0f, GlassMotion.Disappear)
            // Skipped when a re-registration interrupted the disappearance.
            if (shape.isRemoving) shapes.remove(key)
        }
    }

    internal fun press(pointInContainer: Offset) {
        pressPoint = pointInContainer
        animationScope.launch { pressProgress.animateTo(1f, GlassMotion.PressIn) }
    }

    internal fun release() {
        animationScope.launch { pressProgress.animateTo(0f, GlassMotion.Release) }
    }
}

/**
 * Remembers a [LiquidGlassContainerState] bound to [providerState]. Pass it to
 * [liquidGlassContainer] (or the [LiquidGlassContainer] composable) and to the
 * [glassEffect] modifier of every glass child inside it.
 */
@Composable
public fun rememberLiquidGlassContainerState(
    providerState: LiquidGlassProviderState,
): LiquidGlassContainerState {
    val animationScope = rememberCoroutineScope()
    return remember(providerState) {
        LiquidGlassContainerState(providerState, animationScope)
    }
}

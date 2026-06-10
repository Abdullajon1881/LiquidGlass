package dev.liquidglass.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import dev.liquidglass.core.GlassRenderTier

/**
 * Connects one backdrop to any number of glass elements.
 *
 * Apply [liquidGlassProvider] to the content that lives *behind* the glass; it
 * records that content into a [GraphicsLayer] once per frame. Every
 * [liquidGlass] element and [LiquidGlassContainer] that holds this state then
 * re-projects the recorded backdrop through its own blur/refraction pipeline.
 *
 * Glass elements must be drawn after (on top of) the provider, and both must be
 * attached to the same window.
 */
@Stable
public class LiquidGlassProviderState {

    /**
     * Optional fidelity cap applied to every glass element using this state, on
     * top of [LocalLiquidGlassTier]. Use it to honor reduced-transparency
     * accessibility settings or battery saver. Requests can lower the tier but
     * never exceed device capability.
     */
    public var requestedTier: GlassRenderTier? by mutableStateOf(null)

    internal var contentLayer: GraphicsLayer? by mutableStateOf(null)

    internal var coordinates: LayoutCoordinates? = null
        private set

    /**
     * Bumped whenever the provider moves in the window so consumers that did not
     * move themselves (for example fixed chrome above a scrolling provider) still
     * recompute their relative offset.
     */
    internal var positionTick: Int by mutableIntStateOf(0)
        private set

    internal fun onProviderPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        positionTick++
    }

    internal fun onProviderDetached() {
        coordinates = null
        contentLayer = null
    }
}

/** Remembers a [LiquidGlassProviderState] tied to this composition. */
@Composable
public fun rememberLiquidGlassProviderState(): LiquidGlassProviderState =
    remember { LiquidGlassProviderState() }

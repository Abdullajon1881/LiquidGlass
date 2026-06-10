package dev.liquidglass.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * The silhouette of a glass element.
 *
 * Liquid Glass shapes are analytic rounded rectangles — that is what allows the
 * shader to compute exact signed distances, refraction normals, and smooth shape
 * merging. Capsules and circles are rounded rectangles whose corner radius equals
 * half the smaller dimension, matching the iOS defaults.
 */
@Immutable
public sealed interface GlassShape {

    /** A pill: fully rounded short edges. The default, like iOS. */
    public data object Capsule : GlassShape

    /** A circle when the element is square; otherwise identical to [Capsule]. */
    public data object Circle : GlassShape

    /** A rounded rectangle with an explicit [cornerRadius]. */
    public data class RoundedRectangle(public val cornerRadius: Dp) : GlassShape {
        init {
            require(cornerRadius >= 0.dp) { "cornerRadius must be >= 0, got $cornerRadius" }
        }
    }
}

/**
 * Resolves the corner radius in pixels for an element of [size]. The result is
 * always clamped to half the smaller dimension so the SDF stays well-formed.
 */
internal fun GlassShape.resolveCornerRadiusPx(size: Size, density: Density): Float {
    val maxRadius = min(size.width, size.height) / 2f
    return when (this) {
        GlassShape.Capsule, GlassShape.Circle -> maxRadius
        is GlassShape.RoundedRectangle ->
            min(with(density) { cornerRadius.toPx() }, maxRadius)
    }
}

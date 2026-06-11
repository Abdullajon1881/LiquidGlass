package dev.liquidglass.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Edge refraction parameters — the optical core of Liquid Glass.
 *
 * A band of [height] along the rim bends the backdrop with a circular lens
 * profile, displacing each sample by up to [amount] toward the shape center
 * (negative values bend outward). Zero on either axis disables refraction.
 */
@Immutable
public class GlassRefraction(
    public val height: Dp = 12.dp,
    public val amount: Dp = 16.dp,
) {
    init {
        require(height >= 0.dp) { "refraction height must be >= 0, got $height" }
    }

    override fun equals(other: Any?): Boolean =
        other is GlassRefraction && other.height == height && other.amount == amount

    override fun hashCode(): Int = 31 * height.hashCode() + amount.hashCode()

    override fun toString(): String = "GlassRefraction(height=$height, amount=$amount)"

    public companion object {
        /** The standard lens: a 12dp band bending up to 16dp. */
        public val Default: GlassRefraction = GlassRefraction()

        /** No refraction at all — flat frosted glass. */
        public val None: GlassRefraction = GlassRefraction(height = 0.dp, amount = 0.dp)
    }
}

/**
 * The specular rim light that gives glass its physical edge.
 *
 * A band of [width] hugging the contour brightens where the surface normal
 * aligns with the light axis at [lightAngleDegrees] (screen-space, 0° = from the
 * right, 270° = from above). The highlight appears on both the lit and opposite
 * edges, like a real polished rim. [alpha] scales the intensity; zero disables it.
 */
@Immutable
public class GlassHighlight(
    public val width: Dp = 2.5.dp,
    public val alpha: Float = 0.55f,
    public val lightAngleDegrees: Float = 245f,
) {
    init {
        require(width >= 0.dp) { "highlight width must be >= 0, got $width" }
        require(alpha in 0f..1f) { "highlight alpha must be in 0..1, got $alpha" }
    }

    override fun equals(other: Any?): Boolean = other is GlassHighlight &&
        other.width == width && other.alpha == alpha &&
        other.lightAngleDegrees == lightAngleDegrees

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + lightAngleDegrees.hashCode()
        return result
    }

    override fun toString(): String =
        "GlassHighlight(width=$width, alpha=$alpha, lightAngleDegrees=$lightAngleDegrees)"

    public companion object {
        /** Soft top-left key light, the iOS look. */
        public val Default: GlassHighlight = GlassHighlight()

        /** No rim light. */
        public val None: GlassHighlight = GlassHighlight(alpha = 0f)
    }
}

/**
 * The complete appearance of a Liquid Glass element.
 *
 * Styles are immutable; derive variants with [copy], [tinted] or [interactive].
 * Start from a preset — [Regular] for chrome over arbitrary content, [Clear] for
 * media-rich backdrops — and adjust only what the design calls for.
 *
 * @param shape the analytic silhouette, see [GlassShape].
 * @param blurRadius backdrop blur radius; the frostiness of the glass.
 * @param refraction edge lensing, see [GlassRefraction].
 * @param saturation backdrop saturation boost (1 = unchanged). Liquid Glass
 *   slightly over-saturates so color bleeds through the frost.
 * @param tint optional surface color; its alpha is the blend strength.
 * @param highlight specular rim, see [GlassHighlight].
 * @param noiseAlpha strength of the anti-banding grain (0..1, keep tiny).
 * @param chromaticAberration 0..1 RGB dispersion along the lens edge. Tasteful
 *   at 0.3; a prism at 1.
 * @param isInteractive when true the element responds to touch with a gel-like
 *   press: a springy scale, a local bulge in the lens, and a brighter rim.
 * @param fallbackScrim scrim color for the lowest rendering tier (API < 31).
 *   [Color.Unspecified] derives one from [tint] or neutral white.
 */
@Immutable
public class GlassStyle(
    public val shape: GlassShape = GlassShape.Capsule,
    public val blurRadius: Dp = 20.dp,
    public val refraction: GlassRefraction = GlassRefraction.Default,
    public val saturation: Float = 1.5f,
    public val tint: Color = Color.Unspecified,
    public val highlight: GlassHighlight = GlassHighlight.Default,
    public val noiseAlpha: Float = 0.015f,
    public val chromaticAberration: Float = 0f,
    public val isInteractive: Boolean = false,
    public val fallbackScrim: Color = Color.Unspecified,
) {
    init {
        require(blurRadius >= 0.dp) { "blurRadius must be >= 0, got $blurRadius" }
        require(saturation >= 0f) { "saturation must be >= 0, got $saturation" }
        require(noiseAlpha in 0f..1f) { "noiseAlpha must be in 0..1, got $noiseAlpha" }
        require(chromaticAberration in 0f..1f) {
            "chromaticAberration must be in 0..1, got $chromaticAberration"
        }
    }

    /** Returns a copy with [tint] applied — the iOS `.tint(...)` affordance. */
    public fun tinted(tint: Color): GlassStyle = copy(tint = tint)

    /** Returns a copy that reacts to touch — the iOS `.interactive()` affordance. */
    public fun interactive(enabled: Boolean = true): GlassStyle = copy(isInteractive = enabled)

    public fun copy(
        shape: GlassShape = this.shape,
        blurRadius: Dp = this.blurRadius,
        refraction: GlassRefraction = this.refraction,
        saturation: Float = this.saturation,
        tint: Color = this.tint,
        highlight: GlassHighlight = this.highlight,
        noiseAlpha: Float = this.noiseAlpha,
        chromaticAberration: Float = this.chromaticAberration,
        isInteractive: Boolean = this.isInteractive,
        fallbackScrim: Color = this.fallbackScrim,
    ): GlassStyle = GlassStyle(
        shape = shape,
        blurRadius = blurRadius,
        refraction = refraction,
        saturation = saturation,
        tint = tint,
        highlight = highlight,
        noiseAlpha = noiseAlpha,
        chromaticAberration = chromaticAberration,
        isInteractive = isInteractive,
        fallbackScrim = fallbackScrim,
    )

    override fun equals(other: Any?): Boolean = other is GlassStyle &&
        other.shape == shape &&
        other.blurRadius == blurRadius &&
        other.refraction == refraction &&
        other.saturation == saturation &&
        other.tint == tint &&
        other.highlight == highlight &&
        other.noiseAlpha == noiseAlpha &&
        other.chromaticAberration == chromaticAberration &&
        other.isInteractive == isInteractive &&
        other.fallbackScrim == fallbackScrim

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + blurRadius.hashCode()
        result = 31 * result + refraction.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + tint.hashCode()
        result = 31 * result + highlight.hashCode()
        result = 31 * result + noiseAlpha.hashCode()
        result = 31 * result + chromaticAberration.hashCode()
        result = 31 * result + isInteractive.hashCode()
        result = 31 * result + fallbackScrim.hashCode()
        return result
    }

    override fun toString(): String = "GlassStyle(shape=$shape, blurRadius=$blurRadius, " +
        "refraction=$refraction, saturation=$saturation, tint=$tint, highlight=$highlight, " +
        "noiseAlpha=$noiseAlpha, chromaticAberration=$chromaticAberration, " +
        "isInteractive=$isInteractive, fallbackScrim=$fallbackScrim)"

    public companion object {
        /**
         * The everyday material: frosty, saturated, with the standard lens.
         * The equivalent of iOS `.regular` glass.
         */
        public val Regular: GlassStyle = GlassStyle()

        /**
         * A more transparent variant for media-rich backdrops where the content
         * should stay recognizable — lighter blur, gentler saturation.
         * The equivalent of iOS `.clear` glass.
         */
        public val Clear: GlassStyle = GlassStyle(
            blurRadius = 6.dp,
            saturation = 1.2f,
            noiseAlpha = 0.01f,
        )

        /**
         * Tinted, attention-drawing glass for primary actions, like the iOS
         * prominent button style. [tint]'s alpha is the blend strength; pass a
         * translucent color (for example `accent.copy(alpha = 0.5f)`).
         */
        public fun prominent(tint: Color): GlassStyle = Regular.copy(
            tint = tint,
            highlight = GlassHighlight(width = 2.5.dp, alpha = 0.7f),
        )
    }
}

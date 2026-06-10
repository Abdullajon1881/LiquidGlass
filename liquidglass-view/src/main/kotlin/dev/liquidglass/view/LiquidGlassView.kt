package dev.liquidglass.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import dev.liquidglass.core.GlassRenderTier

/**
 * Liquid Glass for the classic View system: a [FrameLayout] whose background is
 * living glass over the backdrop captured by a sibling
 * [LiquidGlassProviderLayout] (or any [LiquidGlassBackdropSource]).
 *
 * Children draw on top of the glass. All behavior lives in
 * [GlassViewController]; embed that directly when you need glass on a view
 * class you don't control (the React Native module does exactly that).
 *
 * All dimension properties are in dp; every property change invalidates the
 * view automatically.
 */
public open class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** The full glass engine; advanced callers may tune it directly. */
    public val controller: GlassViewController = GlassViewController(this)

    /** The backdrop this glass refracts. Must be a sibling drawn below. */
    public var provider: LiquidGlassBackdropSource? by controller::provider

    /** Corner radius in dp, or [CORNER_RADIUS_CAPSULE] for a capsule/circle. */
    public var cornerRadiusDp: Float by controller::cornerRadiusDp

    /** Backdrop blur radius in dp — the frostiness of the glass. */
    public var blurRadiusDp: Float by controller::blurRadiusDp

    /** Width of the refracting band along the rim, in dp. */
    public var refractionHeightDp: Float by controller::refractionHeightDp

    /** Peak refraction displacement in dp (negative bends outward). */
    public var refractionAmountDp: Float by controller::refractionAmountDp

    /** Backdrop saturation boost; 1 leaves color untouched. */
    public var saturation: Float by controller::saturation

    /** Surface tint; the alpha channel is the blend strength. */
    @setparam:ColorInt
    public var glassTintColor: Int by controller::glassTintColor

    /** 0..1 RGB dispersion along the lens edge. */
    public var chromaticAberration: Float by controller::chromaticAberration

    /** Strength of the anti-banding grain. */
    public var noiseAlpha: Float by controller::noiseAlpha

    /** 0..1 intensity of the specular rim highlight. */
    public var highlightAlpha: Float by controller::highlightAlpha

    /** Thickness of the specular rim in dp. */
    public var highlightWidthDp: Float by controller::highlightWidthDp

    /** Screen-space angle the rim light comes from, in degrees. */
    public var lightAngleDegrees: Float by controller::lightAngleDegrees

    /** Scrim color used by the lowest tiers. */
    @setparam:ColorInt
    public var fallbackScrimColor: Int by controller::fallbackScrimColor

    /** When true the glass answers touch with the gel press response. */
    public var isGlassInteractive: Boolean by controller::isGlassInteractive

    /** Optional fidelity cap (accessibility, battery, tests). */
    public var tierOverride: GlassRenderTier? by controller::tierOverride

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        controller.onHostAttached()
    }

    override fun onDetachedFromWindow() {
        controller.onHostDetached()
        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        controller.handleTouch(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        controller.draw(canvas)
    }
}

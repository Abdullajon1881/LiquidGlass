package dev.liquidglass.view

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.ColorInt
import dev.liquidglass.core.GlassRenderTier
import dev.liquidglass.view.internal.GlassFrame
import dev.liquidglass.view.internal.GlassViewRenderer
import kotlin.math.min

/** Sentinel corner radius: fully rounded capsule/circle. */
public const val CORNER_RADIUS_CAPSULE: Float = -1f

private const val DEFAULT_SCRIM_COLOR = 0x59FFFFFF.toInt() // white @ 35%

/**
 * The embeddable Liquid Glass engine for the View system.
 *
 * [LiquidGlassView] is a ready-made host, but any custom view (a React Native
 * view, a launcher widget host, ...) gains glass by owning a controller and
 * forwarding four calls:
 *
 * ```
 * private val glass = GlassViewController(this)   // setWillNotDraw(false)!
 * override fun onDraw(canvas: Canvas)      { super.onDraw(canvas); glass.draw(canvas) }
 * override fun onAttachedToWindow()        { super.onAttachedToWindow(); glass.onHostAttached() }
 * override fun onDetachedFromWindow()      { glass.onHostDetached(); super.onDetachedFromWindow() }
 * override fun dispatchTouchEvent(e: MotionEvent): Boolean { glass.handleTouch(e); return super.dispatchTouchEvent(e) }
 * ```
 *
 * All dimension properties are in dp; every property change invalidates the
 * host automatically. Rendering degrades through the same tiers as the Compose
 * artifact: full AGSL refraction on API 33+, frosted blur on 31–32, clipped
 * backdrop + scrim on 29–30, plain scrim + rim everywhere else.
 */
public class GlassViewController(private val host: View) {

    private val renderer = GlassViewRenderer()
    private val locationSelf = IntArray(2)
    private val locationProvider = IntArray(2)
    private var pressAmount = 0f
    private var pressX = 0f
    private var pressY = 0f
    private var pressAnimator: ValueAnimator? = null

    /** The backdrop this glass refracts. Must be drawn below the host. */
    public var provider: LiquidGlassBackdropSource? = null
        set(value) {
            if (field === value) return
            field?.unregisterConsumer(host)
            field = value
            if (host.isAttachedToWindow) value?.registerConsumer(host)
            host.invalidate()
        }

    /** Corner radius in dp, or [CORNER_RADIUS_CAPSULE] for a capsule/circle. */
    public var cornerRadiusDp: Float = CORNER_RADIUS_CAPSULE
        set(value) = invalidating(field, value) { field = it }

    /** Backdrop blur radius in dp — the frostiness of the glass. */
    public var blurRadiusDp: Float = 20f
        set(value) = invalidating(field, value) { field = it }

    /** Width of the refracting band along the rim, in dp. */
    public var refractionHeightDp: Float = 10f
        set(value) = invalidating(field, value) { field = it }

    /** Peak refraction displacement in dp (negative bends outward). */
    public var refractionAmountDp: Float = 12f
        set(value) = invalidating(field, value) { field = it }

    /** Backdrop saturation boost; 1 leaves color untouched. */
    public var saturation: Float = 1.5f
        set(value) = invalidating(field, value) { field = it }

    /** Surface tint; the alpha channel is the blend strength. */
    @setparam:ColorInt
    public var glassTintColor: Int = Color.TRANSPARENT
        set(value) = invalidating(field, value) { field = it }

    /** 0..1 RGB dispersion along the lens edge. */
    public var chromaticAberration: Float = 0f
        set(value) = invalidating(field, value) { field = it }

    /** Strength of the anti-banding grain. */
    public var noiseAlpha: Float = 0.015f
        set(value) = invalidating(field, value) { field = it }

    /** 0..1 intensity of the specular rim highlight. */
    public var highlightAlpha: Float = 0.45f
        set(value) = invalidating(field, value) { field = it }

    /** Thickness of the specular rim in dp. */
    public var highlightWidthDp: Float = 2f
        set(value) = invalidating(field, value) { field = it }

    /** Screen-space angle the rim light comes from, in degrees. */
    public var lightAngleDegrees: Float = 245f
        set(value) = invalidating(field, value) { field = it }

    /** Scrim color used by the lowest tiers. */
    @setparam:ColorInt
    public var fallbackScrimColor: Int = DEFAULT_SCRIM_COLOR
        set(value) = invalidating(field, value) { field = it }

    /** When true the glass answers touch with the gel press response. */
    public var isGlassInteractive: Boolean = false

    /** Optional fidelity cap (accessibility, battery, tests). */
    public var tierOverride: GlassRenderTier? = null
        set(value) = invalidating(field, value) { field = it }

    public fun onHostAttached() {
        provider?.registerConsumer(host)
    }

    public fun onHostDetached() {
        provider?.unregisterConsumer(host)
        pressAnimator?.cancel()
        renderer.release()
    }

    /** Observes (never consumes) touch for the gel press response. */
    public fun handleTouch(event: MotionEvent) {
        if (!isGlassInteractive) return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressX = event.x
                pressY = event.y
                animatePress(target = 1f, durationMs = 140L, interpolator = DecelerateInterpolator())
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                animatePress(target = 0f, durationMs = 420L, interpolator = OvershootInterpolator(2.2f))
        }
    }

    /** Draws the glass into [canvas]; call before the host's children draw. */
    public fun draw(canvas: Canvas) {
        if (host.width < 1 || host.height < 1) return
        val frame = buildFrame()
        val backdrop = provider
        val providerNode =
            if (Build.VERSION.SDK_INT >= 29) backdrop?.contentRenderNode else null

        val tier = GlassRenderTier.select(Build.VERSION.SDK_INT, tierOverride)
        val canSample = canvas.isHardwareAccelerated &&
            backdrop?.hasRecording == true && providerNode != null

        when {
            canSample && tier == GlassRenderTier.SHADER && Build.VERSION.SDK_INT >= 33 ->
                renderer.drawEffectGlass(canvas, providerNode!!, frame, useShader = true)
            canSample && tier != GlassRenderTier.SCRIM && Build.VERSION.SDK_INT >= 31 ->
                renderer.drawEffectGlass(canvas, providerNode!!, frame, useShader = false)
            canSample && Build.VERSION.SDK_INT >= 29 ->
                renderer.drawBackdropScrim(canvas, providerNode!!, frame, resolveScrimColor())
            else ->
                renderer.drawPlainScrim(canvas, frame, resolveScrimColor())
        }
    }

    private fun buildFrame(): GlassFrame {
        val density = host.resources.displayMetrics.density
        val width = host.width.toFloat()
        val height = host.height.toFloat()
        val maxRadius = min(width, height) / 2f
        val cornerRadiusPx = if (cornerRadiusDp < 0f) {
            maxRadius
        } else {
            min(cornerRadiusDp * density, maxRadius)
        }
        val relative = relativeToProvider()
        return GlassFrame(
            widthPx = width,
            heightPx = height,
            cornerRadiusPx = cornerRadiusPx,
            blurRadiusPx = blurRadiusDp * density,
            refractionHeightPx = refractionHeightDp * density,
            refractionAmountPx = refractionAmountDp * density,
            saturation = saturation,
            tintColor = glassTintColor,
            chromaticAberration = chromaticAberration,
            noiseAlpha = noiseAlpha,
            highlightAlpha = highlightAlpha * (1f + pressAmount * 0.4f),
            highlightWidthPx = highlightWidthDp * density,
            lightAngleDegrees = lightAngleDegrees,
            pressAmount = pressAmount,
            pressX = pressX,
            pressY = pressY,
            relativeX = relative[0],
            relativeY = relative[1],
        )
    }

    private fun relativeToProvider(): FloatArray {
        val backdrop = provider ?: return floatArrayOf(0f, 0f)
        host.getLocationInWindow(locationSelf)
        backdrop.sourceView.getLocationInWindow(locationProvider)
        return floatArrayOf(
            (locationSelf[0] - locationProvider[0]).toFloat(),
            (locationSelf[1] - locationProvider[1]).toFloat(),
        )
    }

    private fun resolveScrimColor(): Int =
        if (Color.alpha(glassTintColor) > 0) glassTintColor else fallbackScrimColor

    private fun animatePress(target: Float, durationMs: Long, interpolator: Interpolator) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(pressAmount, target).apply {
            duration = durationMs
            this.interpolator = interpolator
            addUpdateListener {
                pressAmount = it.animatedValue as Float
                val scale = 1f + 0.025f * pressAmount
                host.scaleX = scale
                host.scaleY = scale
                host.invalidate()
            }
            start()
        }
    }

    private inline fun <T> invalidating(old: T, new: T, assign: (T) -> Unit) {
        if (old != new) {
            assign(new)
            host.invalidate()
        }
    }
}

package expo.modules.liquidglass

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import dev.liquidglass.view.CORNER_RADIUS_CAPSULE
import dev.liquidglass.view.GlassViewController
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.views.ExpoView

/**
 * Native side of `<LiquidGlassView>`: the glass engine drawn directly on the
 * React Native view (children render above it). The view embeds
 * [GlassViewController] rather than wrapping a child view so React Native's
 * mounting (Paper and Fabric) keeps full ownership of the child index space.
 */
class ExpoLiquidGlassModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoLiquidGlass")

        View(LiquidGlassExpoView::class) {
            Prop("providerId") { view: LiquidGlassExpoView, id: String ->
                view.providerId = id
            }
            Prop("cornerRadius") { view: LiquidGlassExpoView, radius: Float? ->
                view.glass.cornerRadiusDp = radius ?: CORNER_RADIUS_CAPSULE
            }
            Prop("blurRadius") { view: LiquidGlassExpoView, radius: Float? ->
                view.glass.blurRadiusDp = radius ?: 20f
            }
            Prop("refractionHeight") { view: LiquidGlassExpoView, height: Float? ->
                view.glass.refractionHeightDp = height ?: 10f
            }
            Prop("refractionAmount") { view: LiquidGlassExpoView, amount: Float? ->
                view.glass.refractionAmountDp = amount ?: 12f
            }
            Prop("saturation") { view: LiquidGlassExpoView, saturation: Float? ->
                view.glass.saturation = saturation ?: 1.5f
            }
            Prop("tint") { view: LiquidGlassExpoView, color: Int? ->
                view.glass.glassTintColor = color ?: 0
            }
            Prop("chromaticAberration") { view: LiquidGlassExpoView, amount: Float? ->
                view.glass.chromaticAberration = (amount ?: 0f).coerceIn(0f, 1f)
            }
            Prop("noiseAlpha") { view: LiquidGlassExpoView, alpha: Float? ->
                view.glass.noiseAlpha = (alpha ?: 0.015f).coerceIn(0f, 1f)
            }
            Prop("highlightAlpha") { view: LiquidGlassExpoView, alpha: Float? ->
                view.glass.highlightAlpha = (alpha ?: 0.45f).coerceIn(0f, 1f)
            }
            Prop("highlightWidth") { view: LiquidGlassExpoView, width: Float? ->
                view.glass.highlightWidthDp = width ?: 2f
            }
            Prop("lightAngle") { view: LiquidGlassExpoView, degrees: Float? ->
                view.glass.lightAngleDegrees = degrees ?: 245f
            }
            Prop("interactive") { view: LiquidGlassExpoView, interactive: Boolean? ->
                view.glass.isGlassInteractive = interactive ?: false
            }
        }
    }
}

class LiquidGlassExpoView(
    context: Context,
    appContext: AppContext,
) : ExpoView(context, appContext) {

    /** The embedded glass engine; props from JS are applied to it directly. */
    val glass = GlassViewController(this)

    var providerId: String = "default"
        set(value) {
            if (field == value && glass.provider != null) return
            field = value
            if (isAttachedToWindow) LiquidGlassRegistry.resolve(value, glass)
        }

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LiquidGlassRegistry.resolve(providerId, glass)
        glass.onHostAttached()
    }

    override fun onDetachedFromWindow() {
        LiquidGlassRegistry.cancelWaiting(glass)
        glass.onHostDetached()
        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        glass.handleTouch(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        glass.draw(canvas)
    }
}

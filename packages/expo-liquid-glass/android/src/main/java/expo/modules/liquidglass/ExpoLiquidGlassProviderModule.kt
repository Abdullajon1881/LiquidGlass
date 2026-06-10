package expo.modules.liquidglass

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderNode
import android.view.View
import dev.liquidglass.view.BackdropRecorder
import dev.liquidglass.view.LiquidGlassBackdropSource
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.views.ExpoView

/**
 * Native side of `<LiquidGlassProvider>`: hosts the React Native children that
 * form the backdrop and records them with a [BackdropRecorder] so sibling
 * glass views can refract them.
 */
class ExpoLiquidGlassProviderModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoLiquidGlassProvider")

        View(LiquidGlassProviderExpoView::class) {
            Prop("providerId") { view: LiquidGlassProviderExpoView, id: String ->
                view.providerId = id
            }
        }
    }
}

class LiquidGlassProviderExpoView(
    context: Context,
    appContext: AppContext,
) : ExpoView(context, appContext), LiquidGlassBackdropSource {

    private val recorder = BackdropRecorder()

    var providerId: String = "default"
        set(value) {
            if (field == value) return
            if (isAttachedToWindow) {
                LiquidGlassRegistry.unregisterProvider(field, this)
                LiquidGlassRegistry.registerProvider(value, this)
            }
            field = value
        }

    override val sourceView: View
        get() = this

    override val contentRenderNode: RenderNode?
        get() = recorder.contentNode

    override val hasRecording: Boolean
        get() = recorder.hasRecording

    override fun registerConsumer(consumer: View) = recorder.register(consumer)

    override fun unregisterConsumer(consumer: View) = recorder.unregister(consumer)

    override fun dispatchDraw(canvas: Canvas) {
        recorder.drawRecorded(canvas, width, height) { target ->
            super.dispatchDraw(target)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LiquidGlassRegistry.registerProvider(providerId, this)
    }

    override fun onDetachedFromWindow() {
        LiquidGlassRegistry.unregisterProvider(providerId, this)
        recorder.release()
        super.onDetachedFromWindow()
    }
}

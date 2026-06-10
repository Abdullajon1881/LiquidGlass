package dev.liquidglass.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderNode
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/**
 * The View-system backdrop provider: place the content that lives *behind* the
 * glass inside this layout. On Android 10+ with hardware acceleration the
 * children are recorded into a reusable [RenderNode] that every registered
 * [LiquidGlassView] re-projects through its own blur/refraction pipeline.
 *
 * [LiquidGlassView]s must be siblings drawn above this layout (never inside it),
 * exactly like the Compose `liquidGlassProvider` / `liquidGlass` pairing.
 */
public open class LiquidGlassProviderLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), LiquidGlassBackdropSource {

    private val recorder = BackdropRecorder()

    override val sourceView: View
        get() = this

    override val contentRenderNode: RenderNode?
        get() = recorder.contentNode

    override val hasRecording: Boolean
        get() = recorder.hasRecording

    override fun registerConsumer(consumer: View) {
        recorder.register(consumer)
    }

    override fun unregisterConsumer(consumer: View) {
        recorder.unregister(consumer)
    }

    override fun dispatchDraw(canvas: Canvas) {
        recorder.drawRecorded(canvas, width, height) { target ->
            super.dispatchDraw(target)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recorder.release()
    }
}

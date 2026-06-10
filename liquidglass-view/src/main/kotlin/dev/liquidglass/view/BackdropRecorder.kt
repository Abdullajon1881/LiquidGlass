package dev.liquidglass.view

import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Anything that can serve as the backdrop behind [LiquidGlassView]s.
 *
 * Implemented by [LiquidGlassProviderLayout] for plain Android apps and by
 * host-specific containers (the React Native provider view, for example) that
 * cannot extend it. Implementations typically delegate the heavy lifting to a
 * [BackdropRecorder].
 */
public interface LiquidGlassBackdropSource {

    /** The view whose window coordinates anchor the backdrop. */
    public val sourceView: View

    /** The recorded backdrop; null below API 29 or before the first draw. */
    @get:RequiresApi(29)
    public val contentRenderNode: RenderNode?

    /** True once a hardware recording of the backdrop exists. */
    public val hasRecording: Boolean

    public fun registerConsumer(consumer: View)

    public fun unregisterConsumer(consumer: View)
}

/**
 * The reusable backdrop capture engine: records a ViewGroup's children into a
 * [RenderNode] during its draw pass (API 29+, hardware canvas) and keeps every
 * registered [LiquidGlassView] refreshed.
 *
 * Call [drawRecorded] from the owner's `dispatchDraw` and [release] when the
 * owner detaches.
 */
public class BackdropRecorder {

    private val consumers = CopyOnWriteArrayList<View>()

    /** The live recording, exposed through [LiquidGlassBackdropSource]. */
    public var contentNode: RenderNode? = null
        private set

    public val hasRecording: Boolean
        get() = Build.VERSION.SDK_INT >= 29 && contentNode?.hasDisplayList() == true

    public fun register(consumer: View) {
        consumers.addIfAbsent(consumer)
    }

    public fun unregister(consumer: View) {
        consumers.remove(consumer)
    }

    /**
     * Records [drawContent] into the backdrop node and replays it onto [canvas]
     * (or draws straight through when recording isn't possible), then invalidates
     * every registered consumer so their glass refreshes.
     */
    public fun drawRecorded(
        canvas: Canvas,
        width: Int,
        height: Int,
        drawContent: (Canvas) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= 29 && canvas.isHardwareAccelerated &&
            width > 0 && height > 0
        ) {
            val node = contentNode
                ?: RenderNode("LiquidGlassBackdrop").also { contentNode = it }
            node.setPosition(0, 0, width, height)
            val recording = node.beginRecording(width, height)
            try {
                drawContent(recording)
            } finally {
                node.endRecording()
            }
            canvas.drawRenderNode(node)
        } else {
            drawContent(canvas)
        }
        for (consumer in consumers) {
            consumer.postInvalidateOnAnimation()
        }
    }

    public fun release() {
        if (Build.VERSION.SDK_INT >= 29) {
            contentNode?.discardDisplayList()
        }
        contentNode = null
    }
}

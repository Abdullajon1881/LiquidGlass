package expo.modules.liquidglass

import dev.liquidglass.view.GlassViewController
import dev.liquidglass.view.LiquidGlassBackdropSource
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Links providers and glass views across the React Native view tree, where
 * object references cannot travel through props. Providers register under
 * their `providerId`; glass views resolve eagerly and, when the provider has
 * not mounted yet, are parked until it appears (mount order in RN is not
 * guaranteed).
 */
internal object LiquidGlassRegistry {

    private val providers =
        ConcurrentHashMap<String, WeakReference<LiquidGlassBackdropSource>>()
    private val waiting =
        CopyOnWriteArrayList<Pair<String, WeakReference<GlassViewController>>>()

    fun registerProvider(id: String, provider: LiquidGlassBackdropSource) {
        providers[id] = WeakReference(provider)
        waiting.removeAll { (wantedId, ref) ->
            val controller = ref.get() ?: return@removeAll true
            if (wantedId == id) {
                controller.provider = provider
                true
            } else {
                false
            }
        }
    }

    fun unregisterProvider(id: String, provider: LiquidGlassBackdropSource) {
        val current = providers[id]?.get()
        if (current == null || current === provider) {
            providers.remove(id)
        }
    }

    fun resolve(id: String, controller: GlassViewController) {
        val provider = providers[id]?.get()
        if (provider != null) {
            controller.provider = provider
        } else {
            controller.provider = null
            waiting.add(id to WeakReference(controller))
        }
    }

    fun cancelWaiting(controller: GlassViewController) {
        waiting.removeAll { (_, ref) ->
            val waitingController = ref.get()
            waitingController == null || waitingController === controller
        }
    }
}

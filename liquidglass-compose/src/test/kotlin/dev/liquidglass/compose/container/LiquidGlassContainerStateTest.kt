package dev.liquidglass.compose.container

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import dev.liquidglass.compose.LiquidGlassProviderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Drives the container's morph state machine with a synthetic frame clock so the
 * spring animations run to completion deterministically on the JVM.
 */
class LiquidGlassContainerStateTest {

    private class TestFrameClock : MonotonicFrameClock {
        private var timeNanos = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            timeNanos += 16_000_000L
            return onFrame(timeNanos)
        }
    }

    private fun runWithState(
        block: suspend (LiquidGlassContainerState, CoroutineScope) -> Unit,
    ) = runBlocking(TestFrameClock()) {
        val scope = CoroutineScope(coroutineContext + Job(coroutineContext.job))
        val state = LiquidGlassContainerState(LiquidGlassProviderState(), scope)
        block(state, scope)
        scope.coroutineContext.job.cancel()
    }

    private suspend fun CoroutineScope.awaitAnimations() {
        coroutineContext.job.children.forEach { it.join() }
    }

    @Test
    fun `registering a shape animates its morph progress to full`() = runWithState { state, scope ->
        val shape = state.registerShape("a")
        assertThat(shape.progress.value).isLessThan(1f)
        scope.awaitAnimations()
        assertThat(shape.progress.value).isEqualTo(1f)
        assertThat(state.shapes).hasSize(1)
    }

    @Test
    fun `unregistering animates the shape away and removes it`() = runWithState { state, scope ->
        state.registerShape("a")
        scope.awaitAnimations()
        state.unregisterShape("a")
        scope.awaitAnimations()
        assertThat(state.shapes).isEmpty()
    }

    @Test
    fun `re-registering during removal keeps the shape alive`() = runWithState { state, scope ->
        val original = state.registerShape("a")
        scope.awaitAnimations()
        state.unregisterShape("a")
        // Interrupt the disappearance before it finishes.
        val revived = state.registerShape("a")
        scope.awaitAnimations()
        assertThat(revived).isSameInstanceAs(original)
        assertThat(state.shapes).hasSize(1)
        assertThat(original.isRemoving).isFalse()
        assertThat(original.progress.value).isEqualTo(1f)
    }

    @Test
    fun `unregistering an unknown key is a safe no-op`() = runWithState { state, scope ->
        state.unregisterShape("ghost")
        scope.awaitAnimations()
        assertThat(state.shapes).isEmpty()
    }

    @Test
    fun `shapes keep distinct geometry per key`() = runWithState { state, scope ->
        val a = state.registerShape("a")
        val b = state.registerShape("b")
        a.targetRect = Rect(Offset.Zero, Size(100f, 50f))
        b.targetRect = Rect(Offset(200f, 0f), Size(80f, 80f))
        scope.awaitAnimations()
        assertThat(state.shapes).hasSize(2)
        assertThat(state.shapes["a"]!!.targetRect.width).isEqualTo(100f)
        assertThat(state.shapes["b"]!!.targetRect.width).isEqualTo(80f)
    }

    @Test
    fun `press records the touch point and animates pressure in and out`() =
        runWithState { state, scope ->
            state.press(Offset(42f, 24f))
            scope.awaitAnimations()
            assertThat(state.pressPoint).isEqualTo(Offset(42f, 24f))
            assertThat(state.pressProgress.value).isEqualTo(1f)
            state.release()
            scope.awaitAnimations()
            assertThat(state.pressProgress.value).isEqualTo(0f)
        }
}

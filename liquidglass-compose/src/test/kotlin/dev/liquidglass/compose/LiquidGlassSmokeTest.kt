package dev.liquidglass.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.liquidglass.compose.components.GlassButton
import dev.liquidglass.compose.container.LiquidGlassContainer
import dev.liquidglass.compose.container.glassEffect
import dev.liquidglass.compose.container.rememberLiquidGlassContainerState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Composition smoke tests under Robolectric on the SCRIM tier (SDK 30): the
 * full provider/consumer/container machinery attaches, lays out, draws and
 * detaches without crashing. Shader output itself is device territory — these
 * tests pin the lifecycle and wiring.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiquidGlassSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun standaloneGlassOverProviderComposes() {
        compose.setContent {
            val state = rememberLiquidGlassProviderState()
            // Glass elements are siblings drawn above the provider, never inside
            // it — drawing the backdrop layer from within its own recording would
            // be a feedback cycle.
            Box(Modifier.size(240.dp)) {
                Box(Modifier.fillMaxSize().background(Color.Red).liquidGlassProvider(state))
                Box(Modifier.size(120.dp).liquidGlass(state, GlassStyle.Regular)) {
                    BasicText("glass")
                }
            }
        }
        compose.onNodeWithText("glass").assertExists()
    }

    @Test
    fun interactiveTintedGlassComposes() {
        compose.setContent {
            val state = rememberLiquidGlassProviderState()
            Box(Modifier.size(240.dp)) {
                Box(Modifier.fillMaxSize().background(Color.Blue).liquidGlassProvider(state))
                GlassButton(onClick = {}, state = state) {
                    BasicText("press me")
                }
            }
        }
        compose.onNodeWithText("press me").assertExists()
    }

    @Test
    fun containerWithMergingChildrenComposesAndMorphsOnRemoval() {
        var showSecond by mutableStateOf(true)
        compose.setContent {
            val state = rememberLiquidGlassProviderState()
            Box(Modifier.size(240.dp)) {
                Box(Modifier.fillMaxSize().background(Color.Green).liquidGlassProvider(state))
                val container = rememberLiquidGlassContainerState(state)
                LiquidGlassContainer(state = container, spacing = 24.dp) {
                    Box(Modifier.size(64.dp).glassEffect(container, id = "first")) {
                        BasicText("first")
                    }
                    if (showSecond) {
                        Box(Modifier.size(64.dp).glassEffect(container, id = "second")) {
                            BasicText("second")
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("first").assertExists()
        compose.onNodeWithText("second").assertExists()

        showSecond = false
        compose.waitForIdle()
        compose.onNodeWithText("second").assertDoesNotExist()
        compose.onNodeWithText("first").assertExists()
    }
}

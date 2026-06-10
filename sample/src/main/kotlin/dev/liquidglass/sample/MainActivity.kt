package dev.liquidglass.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.liquidglass.compose.liquidGlassProvider
import dev.liquidglass.compose.rememberLiquidGlassProviderState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LiquidGlassDemo() }
    }
}

/**
 * The demo is structured the way every Liquid Glass screen should be:
 * the backdrop (provider) at the bottom of the stack, all glass elements
 * drawn above it as siblings — never inside the provider subtree.
 */
@Composable
private fun LiquidGlassDemo() {
    val glassState = rememberLiquidGlassProviderState()
    Box(Modifier.fillMaxSize()) {
        DemoBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassProvider(glassState),
        )
        GlassOverlay(glassState)
    }
}

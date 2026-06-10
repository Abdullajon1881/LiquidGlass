package dev.liquidglass.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.LiquidGlassProviderState
import dev.liquidglass.compose.liquidGlass

/**
 * A floating capsule bar for primary navigation or actions, in the spirit of the
 * iOS 26 floating tab bar. Place it above scrolling provider content (typically
 * inset from the screen edges) and let the content scroll underneath — the bar
 * refracts whatever passes below it.
 *
 * Width is up to the caller: pass `Modifier.fillMaxWidth()` for an edge-to-edge
 * bar or leave it to wrap its items as a floating pill.
 */
@Composable
public fun GlassBottomBar(
    state: LiquidGlassProviderState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Regular,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .liquidGlass(state, style)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

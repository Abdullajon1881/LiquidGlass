package dev.liquidglass.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.LiquidGlassProviderState
import dev.liquidglass.compose.liquidGlass

/**
 * The simplest glass building block: a box whose background is Liquid Glass.
 *
 * Use it for toolbars, badges, floating panels — anywhere content should sit on
 * a piece of glass without extra behavior. Content draws on top of the glass and
 * is not clipped.
 */
@Composable
public fun GlassSurface(
    state: LiquidGlassProviderState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Regular,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.liquidGlass(state, style),
        contentAlignment = contentAlignment,
        content = content,
    )
}

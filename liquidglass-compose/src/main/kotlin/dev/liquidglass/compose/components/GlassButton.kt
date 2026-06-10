package dev.liquidglass.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.liquidglass.compose.GlassShape
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.LiquidGlassProviderState
import dev.liquidglass.compose.liquidGlass

/**
 * A capsule button made of Liquid Glass — the Android counterpart of the iOS
 * `.glass` button style. Interactive by default: pressing it triggers the gel
 * response in addition to invoking [onClick].
 *
 * The library is design-system agnostic, so the button imposes no text style or
 * content color; provide your own themed content. For a tinted, prominent
 * action pass `GlassStyle.prominent(accent).interactive()` as [style].
 */
@Composable
public fun GlassButton(
    onClick: () -> Unit,
    state: LiquidGlassProviderState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Regular.interactive(),
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .liquidGlass(state, style)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * A circular icon button made of Liquid Glass, sized [diameter] (48dp default —
 * the minimum comfortable touch target).
 */
@Composable
public fun GlassIconButton(
    onClick: () -> Unit,
    state: LiquidGlassProviderState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Regular.copy(shape = GlassShape.Circle).interactive(),
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .liquidGlass(state, style)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

package dev.liquidglass.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.liquidglass.compose.GlassShape
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.LiquidGlassProviderState
import dev.liquidglass.compose.liquidGlass

/** The default card silhouette: a generous 24dp rounded rectangle. */
private val CardShape = GlassShape.RoundedRectangle(cornerRadius = 24.dp)

/**
 * A rounded-rectangle glass card for grouped content floating over a backdrop.
 */
@Composable
public fun GlassCard(
    state: LiquidGlassProviderState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Regular.copy(shape = CardShape),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .liquidGlass(state, style)
            .padding(contentPadding),
        content = content,
    )
}

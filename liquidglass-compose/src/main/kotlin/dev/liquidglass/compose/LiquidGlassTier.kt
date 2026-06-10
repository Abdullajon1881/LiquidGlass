package dev.liquidglass.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import dev.liquidglass.core.GlassRenderTier

/**
 * Caps the rendering fidelity of every glass element in the subtree.
 *
 * `null` (the default) lets each device render the best tier it supports.
 * Provide [GlassRenderTier.SCRIM] to honor a reduced-transparency accessibility
 * preference, or a lower tier in screenshot tests for deterministic output.
 * Requests can only lower fidelity — they never exceed device capability.
 */
public val LocalLiquidGlassTier: ProvidableCompositionLocal<GlassRenderTier?> =
    compositionLocalOf { null }

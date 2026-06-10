package dev.liquidglass.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlassRenderTierTest {

    @Test
    fun `android 13 and newer get the full shader tier`() {
        for (api in listOf(33, 34, 35, 36)) {
            assertThat(GlassRenderTier.select(api)).isEqualTo(GlassRenderTier.SHADER)
        }
    }

    @Test
    fun `android 12 gets the blur tier`() {
        assertThat(GlassRenderTier.select(31)).isEqualTo(GlassRenderTier.BLUR)
        assertThat(GlassRenderTier.select(32)).isEqualTo(GlassRenderTier.BLUR)
    }

    @Test
    fun `everything older falls back to the scrim tier`() {
        for (api in listOf(21, 26, 29, 30)) {
            assertThat(GlassRenderTier.select(api)).isEqualTo(GlassRenderTier.SCRIM)
        }
    }

    @Test
    fun `callers can downgrade below device capability`() {
        assertThat(GlassRenderTier.select(34, requested = GlassRenderTier.BLUR))
            .isEqualTo(GlassRenderTier.BLUR)
        assertThat(GlassRenderTier.select(34, requested = GlassRenderTier.SCRIM))
            .isEqualTo(GlassRenderTier.SCRIM)
    }

    @Test
    fun `callers can never exceed device capability`() {
        assertThat(GlassRenderTier.select(29, requested = GlassRenderTier.SHADER))
            .isEqualTo(GlassRenderTier.SCRIM)
        assertThat(GlassRenderTier.select(31, requested = GlassRenderTier.SHADER))
            .isEqualTo(GlassRenderTier.BLUR)
    }

    @Test
    fun `requesting the available tier is a no-op`() {
        assertThat(GlassRenderTier.select(33, requested = GlassRenderTier.SHADER))
            .isEqualTo(GlassRenderTier.SHADER)
    }
}

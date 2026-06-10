package dev.liquidglass.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlassStyleTest {

    @Test
    fun `regular preset matches iOS regular glass expectations`() {
        val style = GlassStyle.Regular
        assertThat(style.shape).isEqualTo(GlassShape.Capsule)
        assertThat(style.blurRadius).isEqualTo(20.dp)
        assertThat(style.saturation).isGreaterThan(1f)
        assertThat(style.isInteractive).isFalse()
        assertThat(style.tint).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `clear preset is more transparent than regular`() {
        assertThat(GlassStyle.Clear.blurRadius).isLessThan(GlassStyle.Regular.blurRadius)
        assertThat(GlassStyle.Clear.saturation).isLessThan(GlassStyle.Regular.saturation)
    }

    @Test
    fun `prominent preset carries the requested tint`() {
        val accent = Color(0.2f, 0.4f, 1f, 0.5f)
        val style = GlassStyle.prominent(accent)
        assertThat(style.tint).isEqualTo(accent)
        assertThat(style.highlight.alpha).isGreaterThan(GlassHighlight.Default.alpha)
    }

    @Test
    fun `tinted returns a copy and leaves the original untouched`() {
        val base = GlassStyle.Regular
        val tinted = base.tinted(Color.Red)
        assertThat(tinted.tint).isEqualTo(Color.Red)
        assertThat(base.tint).isEqualTo(Color.Unspecified)
        assertThat(tinted.blurRadius).isEqualTo(base.blurRadius)
    }

    @Test
    fun `interactive returns a copy with touch response enabled`() {
        val style = GlassStyle.Regular.interactive()
        assertThat(style.isInteractive).isTrue()
        assertThat(GlassStyle.Regular.isInteractive).isFalse()
        assertThat(style.interactive(false).isInteractive).isFalse()
    }

    @Test
    fun `equal styles compare equal and hash equal`() {
        val a = GlassStyle.Regular.copy(tint = Color.Cyan, chromaticAberration = 0.3f)
        val b = GlassStyle.Regular.copy(tint = Color.Cyan, chromaticAberration = 0.3f)
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `negative blur radius is rejected`() {
        assertThrows<IllegalArgumentException>("blurRadius") {
            GlassStyle(blurRadius = (-1).dp)
        }
    }

    @Test
    fun `negative saturation is rejected`() {
        assertThrows<IllegalArgumentException>("saturation") {
            GlassStyle(saturation = -0.1f)
        }
    }

    @Test
    fun `noise alpha outside the unit range is rejected`() {
        assertThrows<IllegalArgumentException>("noiseAlpha") {
            GlassStyle(noiseAlpha = 1.5f)
        }
    }

    @Test
    fun `chromatic aberration outside the unit range is rejected`() {
        assertThrows<IllegalArgumentException>("chromaticAberration") {
            GlassStyle(chromaticAberration = -0.2f)
        }
    }

    @Test
    fun `refraction with negative height is rejected`() {
        assertThrows<IllegalArgumentException>("refraction height") {
            GlassRefraction(height = (-4).dp)
        }
    }

    @Test
    fun `highlight alpha outside the unit range is rejected`() {
        assertThrows<IllegalArgumentException>("highlight alpha") {
            GlassHighlight(alpha = 2f)
        }
    }

    private inline fun <reified T : Throwable> assertThrows(
        messagePart: String,
        block: () -> Unit,
    ) {
        try {
            block()
            throw AssertionError("expected ${T::class.simpleName}")
        } catch (expected: Throwable) {
            if (expected !is T) throw expected
            assertThat(expected).hasMessageThat().contains(messagePart)
        }
    }
}

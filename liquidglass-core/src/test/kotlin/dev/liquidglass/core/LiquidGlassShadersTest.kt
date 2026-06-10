package dev.liquidglass.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * AGSL compiles on-device at runtime, so these tests act as a structural
 * compile gate: they catch unbalanced braces, GLSL-isms that SkSL rejects,
 * unresolved Kotlin templates, and drift between [GlassUniforms] and the
 * shader source before any device sees the program.
 */
class LiquidGlassShadersTest {

    private val source = LiquidGlassShaders.LIQUID_GLASS

    @Test
    fun `the program has the AGSL entry point`() {
        assertThat(source).contains("half4 main(float2 fragCoord)")
    }

    @Test
    fun `the backdrop input is declared as a shader uniform`() {
        assertThat(source).contains("uniform shader ${GlassUniforms.CONTENT};")
    }

    @Test
    fun `every float uniform constant is declared in the source`() {
        for (name in GlassUniforms.ALL_FLOAT_UNIFORMS) {
            assertThat(source).contains("uniform")
            assertThat(Regex("uniform\\s+\\w+[0-9]?\\s+$name(\\[|;)").containsMatchIn(source))
                .isTrue()
        }
    }

    @Test
    fun `braces and parentheses are balanced`() {
        assertThat(source.count { it == '{' }).isEqualTo(source.count { it == '}' })
        assertThat(source.count { it == '(' }).isEqualTo(source.count { it == ')' })
    }

    @Test
    fun `no GLSL vector types leak in - AGSL is SkSL`() {
        assertThat(Regex("\\bvec[234]\\b").containsMatchIn(source)).isFalse()
        assertThat(Regex("\\bivec[234]\\b").containsMatchIn(source)).isFalse()
    }

    @Test
    fun `no derivative intrinsics - AGSL has none`() {
        assertThat(source).doesNotContain("dFdx")
        assertThat(source).doesNotContain("dFdy")
        assertThat(source).doesNotContain("fwidth")
    }

    @Test
    fun `all Kotlin templates resolved - no stray dollar signs`() {
        assertThat(source).doesNotContain("$")
    }

    @Test
    fun `the scene loop bound matches MAX_SHAPES`() {
        assertThat(source).contains("for (int i = 0; i < ${GlassUniforms.MAX_SHAPES}; i++)")
        assertThat(source).contains("[${GlassUniforms.MAX_SHAPES}];")
    }

    @Test
    fun `shape uniforms are fixed-size arrays sized to MAX_SHAPES`() {
        assertThat(source)
            .contains("uniform float4 ${GlassUniforms.SHAPES}[${GlassUniforms.MAX_SHAPES}];")
        assertThat(source)
            .contains("uniform float ${GlassUniforms.SHAPE_RADII}[${GlassUniforms.MAX_SHAPES}];")
    }

    @Test
    fun `the shader mirrors GlassMath constants`() {
        // Epsilon floor and empty-scene distance must match the Kotlin mirror.
        assertThat(source).contains("0.0001")
        assertThat(source).contains("1000000.0")
    }
}

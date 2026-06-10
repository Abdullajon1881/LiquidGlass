package dev.liquidglass.core

/**
 * AGSL source for the Liquid Glass shader.
 *
 * The shader receives the already blurred and saturated backdrop as the
 * [GlassUniforms.CONTENT] input and performs, per pixel:
 *
 *  1. **Scene SDF** — smooth union (polynomial smooth-min) of up to
 *     [GlassUniforms.MAX_SHAPES] rounded rectangles, which is what lets nearby
 *     glass elements melt into one liquid form.
 *  2. **Edge refraction** — a circular lens profile along the rim displaces the
 *     backdrop sample toward the shape center, bending the world the way a convex
 *     glass slab does.
 *  3. **Gel press** — a Gaussian bulge around the touch point.
 *  4. **Chromatic aberration** — optional RGB dispersion along the lens normal.
 *  5. **Tint, specular rim, grain** — surface color, a thin angle-dependent rim
 *     light, and dither noise that prevents gradient banding.
 *
 * Every formula has a Kotlin twin in [GlassMath]; keep them in lockstep.
 *
 * AGSL constraints honored here: SkSL types only (`float2`/`half4`, never GLSL
 * `vec*`), constant loop bounds, no derivative intrinsics (normals use central
 * differences), and a single `half4 main(float2)` entry point.
 */
public object LiquidGlassShaders {

    private const val MAX_SHAPES: Int = GlassUniforms.MAX_SHAPES

    /** The complete AGSL program for the [GlassRenderTier.SHADER] tier. */
    public val LIQUID_GLASS: String = """
uniform shader ${GlassUniforms.CONTENT};
uniform float4 ${GlassUniforms.SHAPES}[$MAX_SHAPES];
uniform float ${GlassUniforms.SHAPE_RADII}[$MAX_SHAPES];
uniform float ${GlassUniforms.MERGE_SMOOTHING};
uniform float ${GlassUniforms.REFRACTION_HEIGHT};
uniform float ${GlassUniforms.REFRACTION_AMOUNT};
uniform float ${GlassUniforms.CHROMATIC_ABERRATION};
uniform float4 ${GlassUniforms.TINT};
uniform float ${GlassUniforms.NOISE_ALPHA};
uniform float2 ${GlassUniforms.LIGHT_DIRECTION};
uniform float ${GlassUniforms.HIGHLIGHT_ALPHA};
uniform float ${GlassUniforms.HIGHLIGHT_WIDTH};
uniform float ${GlassUniforms.PRESS_AMOUNT};
uniform float2 ${GlassUniforms.PRESS_POINT};

float sdRoundedBox(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - radius;
}

float smin(float a, float b, float k) {
    float kk = max(k, 0.0001);
    float h = clamp(0.5 + 0.5 * (b - a) / kk, 0.0, 1.0);
    return mix(b, a, h) - kk * h * (1.0 - h);
}

float sceneSd(float2 p) {
    float d = 1000000.0;
    for (int i = 0; i < $MAX_SHAPES; i++) {
        float4 s = ${GlassUniforms.SHAPES}[i];
        d = smin(d, sdRoundedBox(p - s.xy, s.zw, ${GlassUniforms.SHAPE_RADII}[i]), ${GlassUniforms.MERGE_SMOOTHING});
    }
    return d;
}

float2 sceneNormal(float2 p) {
    float2 e = float2(1.0, 0.0);
    float gx = sceneSd(p + e.xy) - sceneSd(p - e.xy);
    float gy = sceneSd(p + e.yx) - sceneSd(p - e.yx);
    float2 g = float2(gx, gy);
    float len = length(g);
    if (len < 0.0001) {
        return float2(0.0);
    }
    return g / len;
}

half4 main(float2 fragCoord) {
    float2 p = fragCoord;
    float sd = sceneSd(p);
    float mask = 1.0 - smoothstep(-1.0, 1.0, sd);
    if (mask <= 0.001) {
        return half4(0.0);
    }

    float2 normal = sceneNormal(p);
    float edgeDist = max(-sd, 0.0);

    float lens = 0.0;
    float2 sampleCoord = p;
    if (edgeDist < ${GlassUniforms.REFRACTION_HEIGHT}) {
        float x = 1.0 - edgeDist / max(${GlassUniforms.REFRACTION_HEIGHT}, 0.0001);
        lens = 1.0 - sqrt(max(1.0 - x * x, 0.0));
        sampleCoord = p - normal * (${GlassUniforms.REFRACTION_AMOUNT} * lens);
    }

    if (${GlassUniforms.PRESS_AMOUNT} > 0.001) {
        float2 toPress = p - ${GlassUniforms.PRESS_POINT};
        float falloff = exp(-dot(toPress, toPress) / 8000.0);
        sampleCoord -= toPress * (${GlassUniforms.PRESS_AMOUNT} * 0.08 * falloff);
    }

    float caShift = ${GlassUniforms.CHROMATIC_ABERRATION} * lens * abs(${GlassUniforms.REFRACTION_AMOUNT}) * 0.12;
    float4 color;
    if (caShift > 0.001) {
        float2 caOffset = normal * caShift;
        float cr = float4(${GlassUniforms.CONTENT}.eval(sampleCoord - caOffset)).r;
        float4 cg = float4(${GlassUniforms.CONTENT}.eval(sampleCoord));
        float cb = float4(${GlassUniforms.CONTENT}.eval(sampleCoord + caOffset)).b;
        color = float4(cr, cg.g, cb, cg.a);
    } else {
        color = float4(${GlassUniforms.CONTENT}.eval(sampleCoord));
    }

    color.rgb = mix(color.rgb, ${GlassUniforms.TINT}.rgb, ${GlassUniforms.TINT}.a);

    float rim = 1.0 - smoothstep(0.0, max(${GlassUniforms.HIGHLIGHT_WIDTH}, 0.0001), edgeDist);
    float facing = pow(abs(dot(normal, ${GlassUniforms.LIGHT_DIRECTION})), 1.5);
    float pressBoost = 1.0 + ${GlassUniforms.PRESS_AMOUNT} * 0.6;
    color.rgb += float3(rim * facing * ${GlassUniforms.HIGHLIGHT_ALPHA} * pressBoost);

    float noise = fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453) - 0.5;
    color.rgb += float3(noise * ${GlassUniforms.NOISE_ALPHA});

    color.rgb = clamp(color.rgb, 0.0, 1.0);
    return half4(color * mask);
}
""".trimIndent()
}

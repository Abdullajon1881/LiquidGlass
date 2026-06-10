package dev.liquidglass.core

/**
 * Names and layout constants for every uniform in the Liquid Glass AGSL shader.
 *
 * These constants are the single source of truth shared by [LiquidGlassShaders]
 * (which embeds them in the shader source) and the runtime that calls
 * `RuntimeShader.setFloatUniform`, so a typo can never desynchronize the two —
 * unit tests assert that each name appears in the generated source.
 */
public object GlassUniforms {

    /** Maximum number of shapes a single glass scene (container) can merge. */
    public const val MAX_SHAPES: Int = 8

    /** Floats per packed shape: centerX, centerY, halfWidth, halfHeight. */
    public const val SHAPE_FLOATS: Int = 4

    /** Off-screen center used for unused shape slots so they never affect the SDF. */
    public const val SENTINEL_CENTER: Float = -1_000_000f

    /** The backdrop input shader (blurred, saturated provider content). */
    public const val CONTENT: String = "content"

    /** `float4[MAX_SHAPES]` of packed shapes, see [GlassShapePacker.packShapes]. */
    public const val SHAPES: String = "shapes"

    /** `float[MAX_SHAPES]` corner radii, see [GlassShapePacker.packCornerRadii]. */
    public const val SHAPE_RADII: String = "shapeRadii"

    /** Smooth-union distance (px) — how close shapes must be to merge. */
    public const val MERGE_SMOOTHING: String = "mergeK"

    /** Width (px) of the refracting band along the rim. */
    public const val REFRACTION_HEIGHT: String = "refractionHeight"

    /** Peak refraction displacement (px). Positive bends inward, negative outward. */
    public const val REFRACTION_AMOUNT: String = "refractionAmount"

    /** 0..1 dispersion strength; splits RGB along the refraction direction. */
    public const val CHROMATIC_ABERRATION: String = "chromaticAberration"

    /** `float4` straight (non-premultiplied) RGBA tint; alpha is blend strength. */
    public const val TINT: String = "tint"

    /** Strength of the banding-prevention grain, typically 0.0..0.05. */
    public const val NOISE_ALPHA: String = "noiseAlpha"

    /** `float2` normalized direction the rim light comes from. */
    public const val LIGHT_DIRECTION: String = "lightDir"

    /** 0..1 intensity of the specular rim highlight. */
    public const val HIGHLIGHT_ALPHA: String = "highlightAlpha"

    /** Thickness (px) of the specular rim band. */
    public const val HIGHLIGHT_WIDTH: String = "highlightWidth"

    /** 0..1 progress of the gel press interaction. */
    public const val PRESS_AMOUNT: String = "pressAmount"

    /** `float2` press location in shader-local pixels. */
    public const val PRESS_POINT: String = "pressPoint"

    /** Every scalar/vector uniform name (everything except [CONTENT]). */
    public val ALL_FLOAT_UNIFORMS: List<String> = listOf(
        SHAPES,
        SHAPE_RADII,
        MERGE_SMOOTHING,
        REFRACTION_HEIGHT,
        REFRACTION_AMOUNT,
        CHROMATIC_ABERRATION,
        TINT,
        NOISE_ALPHA,
        LIGHT_DIRECTION,
        HIGHLIGHT_ALPHA,
        HIGHLIGHT_WIDTH,
        PRESS_AMOUNT,
        PRESS_POINT,
    )
}

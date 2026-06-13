package dev.liquidglass.core

/**
 * The three rendering tiers Liquid Glass degrades through, best first.
 *
 * The tier is chosen per device with [select]; callers may request a lower tier
 * (for reduced-transparency accessibility settings, battery saver, or tests) but
 * can never exceed what the platform supports.
 */
public enum class GlassRenderTier {

    /**
     * Full liquid glass: AGSL refraction, specular rim, chromatic aberration,
     * shape merging, blur and saturation. Requires Android 13 (API 33) AGSL.
     */
    SHADER,

    /**
     * Frosted glass: `RenderEffect` blur + saturation with a clipped shape and a
     * drawn rim highlight, but no refraction or merging. Android 12 (API 31–32).
     */
    BLUR,

    /**
     * Translucent scrim over the unblurred backdrop with a rim highlight.
     * Works everywhere (API 21+).
     */
    SCRIM;

    public companion object {

        /** Minimum API level for the [SHADER] tier (AGSL `RuntimeShader`). */
        public const val MIN_API_SHADER: Int = 33

        /** Minimum API level for the [BLUR] tier (`RenderEffect` on render nodes). */
        public const val MIN_API_BLUR: Int = 31

        /**
         * Picks the rendering tier for [apiLevel]. When [requested] is non-null the
         * result is downgraded to it — requests can lower fidelity (accessibility,
         * power saving, tests) but never raise it above device capability.
         */
        public fun select(apiLevel: Int, requested: GlassRenderTier? = null): GlassRenderTier {
            val available = when {
                apiLevel >= MIN_API_SHADER -> SHADER
                apiLevel >= MIN_API_BLUR -> BLUR
                else -> SCRIM
            }
            if (requested == null) return available
            return if (requested.ordinal > available.ordinal) requested else available
        }
    }
}

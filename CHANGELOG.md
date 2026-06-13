# Changelog

## 1.0.0 — 2026-06-10

Initial release.

### liquidglass-core
- AGSL liquid-glass shader: smooth-min SDF union (8 shapes), circular-profile
  edge refraction, gel press bulge, chromatic dispersion, tint, angle-aware
  specular rim, anti-banding grain.
- Line-for-line Kotlin mirror of the shader math (`GlassMath`) with a full
  JVM test suite.
- Shape packing with off-screen sentinels and radius clamping.
- Three-tier render policy (`SHADER` 33+ / `BLUR` 31+ / `SCRIM` 21+) with
  downgrade-only overrides.

### liquidglass-compose
- `Modifier.liquidGlassProvider` / `Modifier.liquidGlass` GraphicsLayer
  backdrop pipeline (modifier-node implementation).
- `GlassStyle` with `Regular`, `Clear`, `prominent()` presets and fluent
  `tinted()` / `interactive()` variants.
- `LiquidGlassContainer` + `Modifier.glassEffect`: liquid shape merging and
  enter/exit morphing, the Android counterpart of iOS `GlassEffectContainer`.
- Gel press interaction (springy scale, shader bulge, rim flare).
- `LocalLiquidGlassTier` fidelity cap for accessibility and tests.
- Foundation-only components: `GlassSurface`, `GlassButton`,
  `GlassIconButton`, `GlassCard`, `GlassBottomBar`.

### liquidglass-view
- `LiquidGlassProviderLayout` + `LiquidGlassView` for classic View apps.
- `BackdropRecorder` / `LiquidGlassBackdropSource` capture abstraction
  (RenderNode, API 29+).
- Embeddable `GlassViewController` engine for custom host views.

### liquid-glass-kit (npm)
- `<LiquidGlassProvider>` / `<LiquidGlassView>` React components with a fully
  typed prop surface.
- Android implementation on `GlassViewController` (Paper- and Fabric-safe).
- iOS implementation on the real `UIGlassEffect` (iOS 26+) with ultra-thin
  material fallback.

### sample
- Demo app: draggable refraction lens, morphing FAB cluster, glass bottom
  bar and title pill over an animated backdrop.

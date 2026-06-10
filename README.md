# LiquidGlass

**Apple's Liquid Glass design language, engineered for Android — Jetpack Compose, classic Views, and React Native/Expo.**

Liquid Glass is the dynamic material Apple introduced across its platforms in June 2025: glass that blurs and refracts the content behind it, reflects light along its rim, reacts to touch like gel, and **melts into neighboring glass** as elements approach each other. This project is a ground-up implementation of that material for the Android ecosystem, built on real optics (signed-distance-field lensing in AGSL) rather than a static blur.

```
liquidglass-core      Pure Kotlin: SDF lens math, AGSL shader, uniform packing, tier logic
├── liquidglass-compose   Jetpack Compose renderer (the flagship)
├── liquidglass-view      Classic View system renderer
└── expo-liquid-glass     React Native / Expo module (real UIGlassEffect on iOS 26+)
```

## Why this one

| | **LiquidGlass** | liquid-glass-react | Kyant0 / backdrop | Haze |
|---|---|---|---|---|
| Platform | Android (Compose + Views) + RN/Expo (Android & iOS) | Web React (Chrome only) | Compose Multiplatform | Compose Multiplatform |
| Edge refraction (true lensing) | ✅ AGSL SDF lens | ✅ SVG displacement | ✅ | ❌ (blur only) |
| **Liquid shape merging / morphing** | ✅ smooth-min SDF container, up to 8 shapes | ❌ | ❌ | ❌ |
| Chromatic dispersion | ✅ | ✅ | ✅ | ❌ |
| Gel press interaction | ✅ scale + local bulge + rim flare | hover/click states | ✅ | ❌ |
| Graceful degradation | ✅ 3 tiers down to API 21 | ❌ breaks outside Chrome | API 33+ only | blur or scrim |
| Classic View system | ✅ | — | ❌ | ❌ |
| React Native / Expo | ✅ (+ native UIGlassEffect on iOS 26+) | ❌ | ❌ | ❌ |
| Design-system coupling | none (foundation-only) | none | none | none |
| Optics unit-tested | ✅ shader math mirrored in Kotlin, 80+ tests | ❌ | ❌ | ✅ |

## How it works

1. **Provider** — `Modifier.liquidGlassProvider(state)` records the backdrop content into a `GraphicsLayer` once per frame (zero extra recompositions; invalidation is tracked by the render graph).
2. **Glass** — each glass element re-projects the recorded backdrop through a `RenderEffect` chain: **blur → saturation boost → AGSL liquid-glass shader**.
3. **The shader** computes a signed distance field of the glass silhouette (the smooth-min union of up to 8 rounded rectangles), then per pixel: refracts the backdrop along a circular lens profile at the rim, bulges around the touch point, splits RGB for dispersion, tints, paints an angle-aware specular rim, and dithers against banding.
4. **Tiers** — Android 13+ (API 33) runs the full shader; Android 12 gets frosted blur + drawn rim; everything back to Android 5.0 gets a clipped backdrop with scrim + rim. You can cap the tier (accessibility/battery) but never exceed device capability.

Every formula in the shader has a line-for-line Kotlin mirror (`GlassMath`) covered by JVM unit tests — the optics are proven, not eyeballed.

## Jetpack Compose

```kotlin
dependencies {
    implementation("io.github.abdullajon1881:liquidglass-compose:1.0.0")
}
```

### A glass toolbar in three lines

```kotlin
val glass = rememberLiquidGlassProviderState()

Box(Modifier.fillMaxSize()) {
    // 1. The backdrop: everything that lives behind the glass.
    ScreenContent(Modifier.fillMaxSize().liquidGlassProvider(glass))

    // 2. Glass elements: siblings drawn above the provider — never inside it.
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(24.dp)
            .liquidGlass(glass, GlassStyle.Regular.interactive())
    ) {
        Text("Liquid Glass", Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
    }
}
```

### Styles

```kotlin
GlassStyle.Regular                          // the everyday material (iOS .regular)
GlassStyle.Clear                            // transparent, for media (iOS .clear)
GlassStyle.prominent(accent)                // tinted primary-action glass
GlassStyle.Regular.tinted(Color.Cyan)       // fluent variants
GlassStyle.Regular.interactive()            // gel press response

GlassStyle(                                 // or tune the optics yourself
    shape = GlassShape.RoundedRectangle(24.dp),
    blurRadius = 20.dp,
    refraction = GlassRefraction(height = 12.dp, amount = 16.dp),
    saturation = 1.5f,
    chromaticAberration = 0.4f,
    highlight = GlassHighlight(width = 2.dp, alpha = 0.5f),
)
```

### Liquid merging — the signature feature

The Android counterpart of Apple's `GlassEffectContainer`. Shapes closer than `spacing` melt into one liquid form; children entering/leaving the composition morph out of and into their neighbors:

```kotlin
val container = rememberLiquidGlassContainerState(glass)

LiquidGlassContainer(container, spacing = 40.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (expanded) {
            ActionButton(Modifier.glassEffect(container, id = "edit", interactive = true))
            ActionButton(Modifier.glassEffect(container, id = "share", interactive = true))
        }
        MainFab(Modifier.glassEffect(container, id = "fab", shape = GlassShape.Circle))
    }
}
```

`Modifier.glassEffect` works at any nesting depth inside the container. Animate child positions normally — the merging follows for free.

### Components (foundation-only, zero Material dependency)

`GlassSurface` · `GlassButton` · `GlassIconButton` · `GlassCard` · `GlassBottomBar`

### Accessibility / fidelity control

```kotlin
CompositionLocalProvider(LocalLiquidGlassTier provides GlassRenderTier.SCRIM) {
    // reduced-transparency subtree: scrim + rim, no live backdrop sampling
}
```

## Classic Views

```kotlin
dependencies {
    implementation("io.github.abdullajon1881:liquidglass-view:1.0.0")
}
```

```kotlin
val provider = LiquidGlassProviderLayout(context).apply { addView(screenContent) }
val glass = LiquidGlassView(context).apply {
    this.provider = provider
    blurRadiusDp = 20f
    refractionAmountDp = 12f
    isGlassInteractive = true
}
root.addView(provider)   // backdrop below
root.addView(glass)      // glass above, as a sibling
```

Need glass on a view class you can't subclass? Embed `GlassViewController` directly — four forwarded calls and any `ViewGroup` becomes glass (this is exactly how the React Native module is built).

## React Native / Expo

```bash
npx expo install expo-liquid-glass
```

```tsx
import { LiquidGlassProvider, LiquidGlassView } from 'expo-liquid-glass';

<View style={{ flex: 1 }}>
  <LiquidGlassProvider style={StyleSheet.absoluteFill}>
    <YourScreen />
  </LiquidGlassProvider>

  <LiquidGlassView style={styles.toolbar} interactive chromaticAberration={0.3}>
    <Text>Liquid Glass</Text>
  </LiquidGlassView>
</View>
```

- **iOS 26+**: renders Apple's real `UIGlassEffect` — the genuine article, not an imitation. Older iOS falls back to ultra-thin material blur.
- **Android**: the full AGSL pipeline from this repo via `liquidglass-view`, with the same tier degradation.
- One JS API for both platforms; Android-only optical props are safely ignored on iOS.

See [packages/expo-liquid-glass](packages/expo-liquid-glass/README.md) for details.

## Rendering tiers

| Tier | Requirements | What you get |
|---|---|---|
| `SHADER` | Android 13+ (API 33) | Refraction, dispersion, merging, gel bulge, rim light, blur, saturation, grain |
| `BLUR` | Android 12 (API 31–32) | Frosted blur + saturation, clipped shape, drawn rim, tint |
| `SCRIM` | Android 5.0+ (API 21) | Translucent scrim over the (clipped) backdrop + rim |

## Performance notes

- One backdrop recording per provider per frame, shared by all glass elements.
- The AGSL program compiles once per node; per-frame work is uniform updates only.
- Glass layers are inflated just enough (`2·blur + |refraction|`) that blur kernels never sample clamped edges.
- Modifier-node implementation (no composed modifiers, no recomposition on scroll — invalidation rides the snapshot/render graph).
- Keep glass to chrome — bars, buttons, cards, overlays. Don't glaze entire screens; Apple doesn't either.

## Rules of the material

1. Glass elements draw **above** the provider, never inside its subtree (a glass node inside the backdrop would sample its own recording).
2. One provider per `LiquidGlassProviderState`.
3. Containers support up to 8 merged shapes — far more than tasteful design needs.
4. Content on glass is yours to style; the library imposes no text color. Verify contrast in both light and dark contexts.

## Project layout

```
liquidglass-core/       Pure Kotlin (JVM): GlassMath, LiquidGlassShaders (AGSL),
                        GlassShapePacker, GlassRenderTier — fully unit tested
liquidglass-compose/    Provider/glass modifier nodes, GlassPainter, container
                        merge/morph, components, Robolectric tests
liquidglass-view/       BackdropRecorder, GlassViewController, LiquidGlassView,
                        LiquidGlassProviderLayout, Robolectric tests
packages/
  expo-liquid-glass/    Expo module: TS API, Android (Kotlin), iOS (Swift)
sample/                 Demo app: draggable lens, morphing FAB cluster,
                        glass bottom bar over an animated backdrop
```

## Building

```bash
./gradlew test                  # core + compose + view test suites (80+ tests)
./gradlew :sample:assembleDebug # demo APK
./gradlew publishToMavenLocal   # required once before building the Expo module
```

## Prior art & credits

- Apple — the [Liquid Glass design language](https://www.apple.com/newsroom/2025/06/apple-introduces-a-delightful-and-elegant-new-software-design/) this implements.
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) and [rdev/liquid-glass-react](https://github.com/rdev/liquid-glass-react) — fellow travelers whose feature sets informed the comparison above.
- [chrisbanes/haze](https://github.com/chrisbanes/haze) — pioneered the GraphicsLayer backdrop pattern on Compose.
- Inigo Quilez — the rounded-box SDF and smooth-min formulations.

## License

Apache License 2.0 — see [LICENSE](LICENSE).

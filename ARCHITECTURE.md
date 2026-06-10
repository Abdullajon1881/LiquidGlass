# LiquidGlass Architecture

This document explains how the library renders Apple's Liquid Glass material on
Android, and why it is built the way it is.

## The rendering pipeline

```
┌────────────────────────────────────────────────────────────────────┐
│ liquidGlassProvider (backdrop content)                             │
│                                                                    │
│   draw() ──► GraphicsLayer.record(content) ──► drawLayer(content)  │
│                      │                                             │
│                      │ shared, recorded once per frame             │
└──────────────────────┼─────────────────────────────────────────────┘
                       ▼
┌────────────────────────────────────────────────────────────────────┐
│ liquidGlass / liquidGlassContainer (each glass element)            │
│                                                                    │
│  glassLayer.record {                                               │
│      translate(inflate − positionInProvider)                       │
│      drawLayer(providerLayer)          // live reference           │
│  }                                                                 │
│  glassLayer.renderEffect =                                         │
│      RuntimeShader(LIQUID_GLASS)       // AGSL, API 33+            │
│        ⤷ chained onto blur + saturation color-matrix               │
│  translate(−inflate) { drawLayer(glassLayer) }                     │
│  drawContent()                         // children above the glass │
└────────────────────────────────────────────────────────────────────┘
```

Key invariants:

- **One recording per provider per frame.** Consumers draw the provider's
  `GraphicsLayer` by reference; re-recording propagates through the render
  graph's dependency tracking without recomposition.
- **The inflate margin** (`2·blur + |refractionAmount| + spacing/2 + 4px`)
  guarantees blur kernels and outward refraction never read clamped edge
  pixels.
- **Positions are computed at draw time** from live `LayoutCoordinates`, and
  the provider bumps a snapshot `positionTick` when it moves so glass that
  didn't move itself (fixed chrome over a scrolling backdrop) still refreshes.
- **Glass must not live inside the provider subtree** — it would draw the
  provider's layer into the very recording that produces it.

## The shader

One AGSL program (`LiquidGlassShaders.LIQUID_GLASS`) renders everything:

1. **Scene SDF** — the polynomial smooth-min union of up to 8 rounded
   rectangles (`shapes[8]`, `shapeRadii[8]`, `mergeK`). Unused slots are parked
   at −10⁶ px where they cannot influence the union. The smooth-min is what
   makes two approaching capsules grow a liquid neck and merge.
2. **Mask** — `1 − smoothstep(−1, 1, sd)` doubles as an antialiased clip, so
   the shader tier needs no geometric clipping at all.
3. **Normals** — central differences over the scene SDF (AGSL has no
   derivative intrinsics). The gradient of the *blended* field is what bends
   light around merge necks correctly.
4. **Refraction** — within `refractionHeight` of the rim, samples displace
   toward the shape center along a circular lens profile
   `1 − √(1 − x²)` — zero slope at the inner edge (flat glass center),
   infinite slope at the rim (the signature edge bend).
5. **Gel press** — a Gaussian bulge around `pressPoint` scaled by
   `pressAmount`, driven by spring animations from the modifier node.
6. **Dispersion** — optional RGB split along the refraction direction,
   proportional to the lens strength so it lives only at the rim.
7. **Tint → rim light → grain** — straight-alpha tint mix; a thin
   angle-aware specular band (`|dot(normal, lightDir)|^1.5`, brighter while
   pressed); hash-noise dither against gradient banding.

Blur and saturation run *before* the shader as platform `RenderEffect`s
(`createBlurEffect` → color-matrix `setSaturation` → chained into the runtime
shader's `content` input) because the platform blur is highly optimized.

### Trusting the shader

AGSL compiles on-device at runtime, so two layers of tests stand in for a
compiler:

- `GlassMath` mirrors every shader formula line-for-line in Kotlin (same
  epsilons), and the JVM suite proves surface positions, normal directions,
  lens monotonicity, smooth-min symmetry/bounds, and that sentinel slots are
  invisible to the union.
- `LiquidGlassShadersTest` is a structural gate: balanced braces, SkSL-only
  types (no GLSL `vec*`), no derivative intrinsics, every uniform declared
  exactly as `GlassUniforms` names it, loop bounds matching `MAX_SHAPES`.

## Tier degradation

`GlassRenderTier.select(apiLevel, requested)` — requests (accessibility,
battery saver, screenshot tests) can lower fidelity, never raise it.

| Tier | API | Pipeline |
|---|---|---|
| SHADER | 33+ | full AGSL program, shader-side mask |
| BLUR | 31–32 | blur + saturation `RenderEffect`, geometric clip, drawn rim, tint wash |
| SCRIM | 21+ | clipped unblurred backdrop + translucent scrim + drawn rim |

The View system adds one nuance: backdrop capture needs `RenderNode`
(API 29+), so 29–30 renders SCRIM *with* a live backdrop and below 29 (or on
software canvases) a plain scrim.

## Containers: merge & morph

`LiquidGlassContainerState` owns a snapshot map of child shapes. Children
(`Modifier.glassEffect(state, id, …)`) register their rects via
`onGloballyPositioned` from any nesting depth; the container draws **one**
glass surface for the union each frame.

- `spacing` maps to the smooth-min `k`: the distance at which shapes melt.
- Enter/exit morphs are springs on each shape's `progress`
  (scale 0.4 → 1 with overshoot in, critically damped out); because the SDF
  blends while a shape is small, it visually emerges from its neighbors.
- Re-registering an id mid-disappearance revives the same shape (the
  interrupted animation cancels the removal).

## Module layering

```
liquidglass-core      pure Kotlin/JVM — zero Android types
   ▲            ▲
compose        view ◄────────── packages/expo-liquid-glass (Android side)
```

- **core** holds everything that must never drift apart: shader source,
  uniform names, packing layout, tier policy, and the math mirror. Both
  renderers consume it, so Compose and Views cannot disagree about optics.
- **compose** is modifier-node based end to end (no composed modifiers): draw
  invalidation rides snapshot reads (`positionTick`, `Animatable.value`,
  the shapes map) instead of recomposition.
- **view** isolates the engine in `GlassViewController` so any host
  `ViewGroup` can embed glass with four forwarded calls. `LiquidGlassView` is
  the convenience host; the React Native view is another.
- **expo-liquid-glass** maps JS props onto the controller on Android. On iOS
  it hosts the real `UIGlassEffect` (26+) or an ultra-thin material blur —
  UIKit samples the backdrop natively, so the provider is a passthrough there.
  Glass draws in the RN view's own `onDraw`, leaving React Native (Paper and
  Fabric) full ownership of child mounting indices.

## Design decisions worth recording

| Decision | Rationale |
|---|---|
| Analytic rounded-rect silhouettes only | exact SDFs ⇒ exact normals, refraction, and merging; arbitrary `Shape` would force raster masks and kill the liquid union |
| Rebuild `RenderEffect` every draw | uniforms are immutable per effect; the shader *program* compiles once, and draws only happen on invalidation |
| Constant-bound shader loop over 8 slots with off-screen sentinels | SkSL requires unrollable loops; branch-free and verified by the math mirror |
| Explicit container state (`rememberLiquidGlassContainerState`) over scope-member modifiers | `@DslMarker` on layout scopes silently breaks scope members under nested `Column`/`Row`; the explicit handle works at any depth and matches `LazyListState` idiom |
| Foundation-only components | any design system, no Material lock-in, no text-color opinions |
| `GlassViewController` extraction | single engine for plain Views and RN hosts without child-index hacks |

## Known limitations (v1)

- Containers don't merge on BLUR/SCRIM tiers (shapes render individually).
- Glass cannot sample across windows (dialogs/popups need their own provider).
- Provider/glass pairs assume no rotation/scale transforms between their
  coordinate spaces (translation and scroll are fine).
- Auto light/dark content adaptation (backdrop luminance sampling) is
  deliberately out of scope for v1 — pixel readback needs a throttled design
  to avoid GPU stalls. Tracked for v1.1.
- iOS Swift sources and the Expo Android module compile inside an Expo app
  build (they need `expo-modules-core` from npm); they are not part of this
  repo's Gradle build.

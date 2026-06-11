# Contributing to LiquidGlass

Thanks for your interest in improving LiquidGlass. This guide covers how the
project is structured, how to build it, and the bar for changes.

## Project layout

| Module | Language | What lives here |
|---|---|---|
| `liquidglass-core` | Pure Kotlin (JVM) | AGSL shader source, SDF/lens math mirror, shape packing, render-tier policy. **Zero Android types.** |
| `liquidglass-compose` | Kotlin + Compose | Provider/glass modifier nodes, painter, container merge/morph, components |
| `liquidglass-view` | Kotlin + Android | `BackdropRecorder`, `GlassViewController`, `LiquidGlassView` |
| `packages/expo-liquid-glass` | TS + Kotlin + Swift | React Native / Expo module |
| `sample` | Kotlin + Compose | Demo app |

`core` is the single source of truth that keeps the Compose and View renderers
optically identical. If you touch the shader, **you must update its Kotlin
mirror in `GlassMath` in the same change**, and vice versa — the tests assume
they agree.

## Building

```bash
./gradlew test                  # core + compose + view unit/Robolectric suites
./gradlew :sample:assembleDebug # demo APK
./gradlew publishToMavenLocal   # required before building the Expo module
```

For the Expo module's TypeScript:

```bash
cd packages/expo-liquid-glass && npm install && npx tsc --noEmit
```

Toolchain: JDK 17+ (the Android Studio JBR is fine), Android SDK 35, Gradle
wrapper 8.11.1, Kotlin 2.1.0.

## Standards for a change

- **Tests.** New optics get a `GlassMath` test; new public API gets a behavior
  test; new rendering paths get a Robolectric smoke test. Don't drop coverage.
- **`explicitApi()` is on** for the library modules — every public declaration
  needs an explicit visibility and a KDoc explaining *why*, not just *what*.
- **Immutability.** Public types are immutable; derive variants with `copy`.
- **No new dependencies** in `core` (it must stay Android-free) without a strong
  reason. The compose/view modules avoid Material — components are foundation-only.
- **Keep it small.** Files under ~400 lines, functions focused. Match the
  surrounding style.

## On-device verification

AGSL compiles on-device, so visual or shader changes should be verified on a
real **API 33+** device or emulator and, ideally, accompanied by before/after
screenshots in the PR. The structural shader tests catch syntax-class errors
but cannot judge how it looks.

## Pull requests

1. Branch from `main`.
2. Make sure `./gradlew test` and `:sample:assembleDebug` are green.
3. Fill in the PR template, including the test plan and any screenshots.
4. One logical change per PR.

## Reporting bugs & ideas

Use the issue templates. For visual bugs, include device model, Android
version, and a screenshot — the rendering tier depends on the API level, so
that context is essential.

## License

By contributing you agree your contributions are licensed under the
[Apache License 2.0](LICENSE).

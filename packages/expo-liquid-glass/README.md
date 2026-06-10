# expo-liquid-glass

Apple's **Liquid Glass** for React Native and Expo.

- **iOS 26+** — renders Apple's real `UIGlassEffect`: the genuine system
  material, not an imitation. Older iOS falls back to an ultra-thin material
  blur.
- **Android 13+** — the full AGSL refraction pipeline from
  [LiquidGlass](../../README.md): SDF edge lensing, chromatic dispersion,
  specular rim, gel press. Android 12 gets frosted blur; Android 10–11 a
  clipped backdrop with scrim; older versions a plain scrim.
- One typed JS API for both platforms.

## Install

```bash
npx expo install expo-liquid-glass
```

> **Android requirement (until artifacts ship on Maven Central):** the module
> depends on `io.github.abdullajon1881:liquidglass-view`. From the LiquidGlass
> repo root run `./gradlew publishToMavenLocal` once, and keep `mavenLocal()`
> in the repositories list (the module's build.gradle already includes it).

## Usage

```tsx
import { StyleSheet, Text, View } from 'react-native';
import { LiquidGlassProvider, LiquidGlassView } from 'expo-liquid-glass';

export default function Screen() {
  return (
    <View style={{ flex: 1 }}>
      {/* 1. The backdrop: everything that lives behind the glass. */}
      <LiquidGlassProvider style={StyleSheet.absoluteFill}>
        <YourContent />
      </LiquidGlassProvider>

      {/* 2. Glass elements: siblings above the provider, never inside it. */}
      <LiquidGlassView style={styles.bar} interactive chromaticAberration={0.3}>
        <Text style={styles.label}>Liquid Glass</Text>
      </LiquidGlassView>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    position: 'absolute',
    bottom: 32,
    alignSelf: 'center',
    paddingHorizontal: 24,
    paddingVertical: 14,
  },
  label: { color: 'white', fontWeight: '600' },
});
```

## Props

`LiquidGlassProvider`

| Prop | Type | Default | Notes |
|---|---|---|---|
| `providerId` | `string` | `"default"` | Links glass views to this backdrop (Android; iOS samples natively) |

`LiquidGlassView`

| Prop | Type | Default | Platform |
|---|---|---|---|
| `providerId` | `string` | `"default"` | Android |
| `cornerRadius` | `number` (dp) | capsule | both |
| `tint` | `ColorValue` | — | both |
| `interactive` | `boolean` | `false` | both |
| `blurRadius` | `number` (dp) | `20` | Android |
| `refractionHeight` | `number` (dp) | `10` | Android |
| `refractionAmount` | `number` (dp) | `12` | Android |
| `saturation` | `number` | `1.5` | Android |
| `chromaticAberration` | `number` 0..1 | `0` | Android |
| `noiseAlpha` | `number` 0..1 | `0.015` | Android |
| `highlightAlpha` | `number` 0..1 | `0.45` | Android |
| `highlightWidth` | `number` (dp) | `2` | Android |
| `lightAngle` | `number` (deg) | `245` | Android |

On iOS 26+ the system glass manages its own optics, so the Android-only props
are accepted and ignored — shared component code just works.

## Rules

1. Glass views are **siblings above** the provider, never its children.
2. Mount order is free — glass views wait for their provider if it mounts
   later.
3. Use distinct `providerId`s only when several independent backdrops coexist.

## License

Apache-2.0

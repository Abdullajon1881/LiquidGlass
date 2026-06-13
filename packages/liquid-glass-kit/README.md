# liquid-glass-kit

**Liquid Glass** for Android, React Native and Expo — **inspired by Apple's
material, built natively for Kotlin/Android and RN/Expo.** One self-contained
package, zero native setup.

- **Android 13+** — the full AGSL refraction pipeline: SDF edge lensing,
  chromatic dispersion, specular rim, gel press. Android 12 gets frosted blur;
  Android 10–11 a clipped backdrop with scrim; older versions a plain scrim.
  The entire Android engine is **vendored inside this package** — there is no
  external Maven dependency to install.
- **Cross-platform safe** — on iOS and web the components fall back to a
  transparent passthrough `View`: children render normally, nothing crashes.
  The glass is Android-only; the package is still safe to ship in any
  cross-platform app. Use the exported `isLiquidGlassSupported` boolean to
  branch your UI when you want a different treatment off Android.
- One typed JS API.

## Install

```bash
npx expo install liquid-glass-kit
```

This is a native module, so it requires a development build (it does not run in
Expo Go). After installing, rebuild your app:

```bash
npx expo prebuild
npx expo run:android   # or: npx expo run:ios
```

## Usage

```tsx
import { StyleSheet, Text, View } from 'react-native';
import { LiquidGlassProvider, LiquidGlassView } from 'liquid-glass-kit';

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
| `providerId` | `string` | `"default"` | Links glass views to this backdrop |

`LiquidGlassView`

| Prop | Type | Default |
|---|---|---|
| `providerId` | `string` | `"default"` |
| `cornerRadius` | `number` (dp) | capsule |
| `tint` | `ColorValue` | — |
| `interactive` | `boolean` | `false` |
| `blurRadius` | `number` (dp) | `20` |
| `refractionHeight` | `number` (dp) | `12` |
| `refractionAmount` | `number` (dp) | `16` |
| `saturation` | `number` | `1.5` |
| `chromaticAberration` | `number` 0..1 | `0` |
| `noiseAlpha` | `number` 0..1 | `0.015` |
| `highlightAlpha` | `number` 0..1 | `0.55` |
| `highlightWidth` | `number` (dp) | `2.5` |
| `lightAngle` | `number` (deg) | `245` |

These props drive the Android rendering. On iOS and web the view is a plain
passthrough, so they're accepted and ignored — shared component code just works.

```ts
import { isLiquidGlassSupported } from 'liquid-glass-kit';
// true on Android, false elsewhere
```

## Rules

1. Glass views are **siblings above** the provider, never its children.
2. Mount order is free — glass views wait for their provider if it mounts
   later.
3. Use distinct `providerId`s only when several independent backdrops coexist.

## License

Apache-2.0

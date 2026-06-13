import { requireNativeViewManager } from 'expo-modules-core';
import * as React from 'react';
import { Platform, View, processColor } from 'react-native';

import type {
  LiquidGlassProviderProps,
  LiquidGlassViewProps,
} from './ExpoLiquidGlass.types';

export type {
  LiquidGlassProviderProps,
  LiquidGlassViewProps,
} from './ExpoLiquidGlass.types';

/**
 * Liquid Glass is rendered by a native Android module. On every other platform
 * (iOS, web) the components degrade to a plain passthrough `View`: children
 * render normally with no glass effect, and nothing crashes. This keeps the
 * package safe to drop into any cross-platform React Native / Expo app while
 * the glass itself remains Android-only.
 */
const isAndroid = Platform.OS === 'android';

const NativeProviderView: React.ComponentType<LiquidGlassProviderProps> | null =
  isAndroid ? requireNativeViewManager('ExpoLiquidGlassProvider') : null;

type NativeGlassProps = Omit<LiquidGlassViewProps, 'tint'> & {
  tint?: ReturnType<typeof processColor>;
};

const NativeGlassView: React.ComponentType<NativeGlassProps> | null = isAndroid
  ? requireNativeViewManager('ExpoLiquidGlass')
  : null;

/** True when running on a platform that renders real Liquid Glass (Android). */
export const isLiquidGlassSupported: boolean = isAndroid;

/**
 * The backdrop behind your glass. Wrap the content that should be visible
 * through the glass — screens, lists, media — in this view, and render
 * `LiquidGlassView`s as siblings above it.
 *
 * ```tsx
 * <View style={{ flex: 1 }}>
 *   <LiquidGlassProvider style={StyleSheet.absoluteFill}>
 *     <YourContent />
 *   </LiquidGlassProvider>
 *   <LiquidGlassView style={styles.toolbar} interactive>
 *     <Text>Liquid Glass</Text>
 *   </LiquidGlassView>
 * </View>
 * ```
 *
 * On non-Android platforms this is a plain `View` (transparent passthrough).
 */
export function LiquidGlassProvider({
  providerId = 'default',
  ...rest
}: LiquidGlassProviderProps) {
  if (!NativeProviderView) {
    return <View {...rest} />;
  }
  return <NativeProviderView providerId={providerId} {...rest} />;
}

/**
 * A Liquid Glass surface. Children render on top of the glass.
 *
 * On Android 13+ this runs the full AGSL refraction pipeline (Android 12 gets
 * frosted blur; older versions a clipped backdrop with scrim). On non-Android
 * platforms it is a plain `View` — children render with no glass effect — so
 * the same component is safe across a cross-platform app.
 */
export function LiquidGlassView({
  providerId = 'default',
  tint,
  ...rest
}: LiquidGlassViewProps) {
  if (!NativeGlassView) {
    return <View {...rest} />;
  }
  return (
    <NativeGlassView
      providerId={providerId}
      tint={tint != null ? processColor(tint) : undefined}
      {...rest}
    />
  );
}

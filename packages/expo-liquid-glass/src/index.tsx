import { requireNativeViewManager } from 'expo-modules-core';
import * as React from 'react';
import { processColor } from 'react-native';

import type {
  LiquidGlassProviderProps,
  LiquidGlassViewProps,
} from './ExpoLiquidGlass.types';

export type {
  LiquidGlassProviderProps,
  LiquidGlassViewProps,
} from './ExpoLiquidGlass.types';

const NativeProviderView: React.ComponentType<LiquidGlassProviderProps> =
  requireNativeViewManager('ExpoLiquidGlassProvider');

type NativeGlassProps = Omit<LiquidGlassViewProps, 'tint'> & {
  tint?: ReturnType<typeof processColor>;
};

const NativeGlassView: React.ComponentType<NativeGlassProps> =
  requireNativeViewManager('ExpoLiquidGlass');

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
 */
export function LiquidGlassProvider({
  providerId = 'default',
  ...rest
}: LiquidGlassProviderProps) {
  return <NativeProviderView providerId={providerId} {...rest} />;
}

/**
 * A Liquid Glass surface. Children render on top of the glass.
 *
 * iOS 26+ uses Apple's native `UIGlassEffect`; Android 13+ runs the AGSL
 * refraction pipeline; both degrade gracefully on older OS versions. See
 * `LiquidGlassViewProps` for which tuning props apply per platform.
 */
export function LiquidGlassView({
  providerId = 'default',
  tint,
  ...rest
}: LiquidGlassViewProps) {
  return (
    <NativeGlassView
      providerId={providerId}
      tint={tint != null ? processColor(tint) : undefined}
      {...rest}
    />
  );
}

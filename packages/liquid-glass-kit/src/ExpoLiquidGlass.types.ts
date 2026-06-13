import type { ColorValue, ViewProps } from 'react-native';

/**
 * Props for the backdrop provider. Content rendered inside is what the glass
 * elements sample, blur and refract.
 *
 * On iOS the provider is a plain passthrough view (UIKit samples the backdrop
 * natively); on Android it records its children into a reusable RenderNode.
 */
export interface LiquidGlassProviderProps extends ViewProps {
  /**
   * Links glass views to this backdrop. Use distinct ids when several
   * independent backdrops coexist. Defaults to `"default"`.
   */
  providerId?: string;
}

/**
 * Props for a Liquid Glass surface.
 *
 * On iOS 26+ this renders Apple's real `UIGlassEffect`; the system manages
 * blur, refraction and highlights itself, so the optical tuning props
 * (`blurRadius`, `refraction*`, `saturation`, `chromaticAberration`,
 * `noiseAlpha`, `highlight*`, `lightAngle`) apply to **Android only**. Older
 * iOS falls back to `UIBlurEffect` ultra-thin material.
 *
 * On Android 13+ the full AGSL pipeline runs (refraction, dispersion, rim
 * light, grain); Android 12 gets frosted blur, and everything back to
 * Android 10 gets a clipped backdrop with a scrim.
 */
export interface LiquidGlassViewProps extends ViewProps {
  /** Backdrop to refract — must match a provider's `providerId`. Android only. */
  providerId?: string;

  /** Corner radius in dp. Omit for a capsule (fully rounded). */
  cornerRadius?: number;

  /** Backdrop blur radius in dp — the frostiness. Android only. Default 20. */
  blurRadius?: number;

  /** Width of the refracting band along the rim, in dp. Android only. Default 10. */
  refractionHeight?: number;

  /** Peak refraction displacement in dp (negative bends outward). Android only. Default 12. */
  refractionAmount?: number;

  /** Backdrop saturation boost; 1 = unchanged. Android only. Default 1.5. */
  saturation?: number;

  /** Surface tint; the alpha channel is the blend strength. */
  tint?: ColorValue;

  /** 0..1 RGB dispersion along the lens edge. Android only. Default 0. */
  chromaticAberration?: number;

  /** Strength of the anti-banding grain, 0..1. Android only. Default 0.015. */
  noiseAlpha?: number;

  /** 0..1 intensity of the specular rim highlight. Android only. Default 0.45. */
  highlightAlpha?: number;

  /** Thickness of the specular rim in dp. Android only. Default 2. */
  highlightWidth?: number;

  /** Screen-space angle the rim light comes from, in degrees. Android only. Default 245. */
  lightAngle?: number;

  /** Respond to touch with the gel press (scale, bulge, brighter rim). */
  interactive?: boolean;
}

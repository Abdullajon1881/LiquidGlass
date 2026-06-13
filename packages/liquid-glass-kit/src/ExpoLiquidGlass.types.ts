import type { ColorValue, ViewProps } from 'react-native';

/**
 * Props for the backdrop provider. Content rendered inside is what the glass
 * elements sample, blur and refract.
 *
 * On Android it records its children into a reusable RenderNode that glass
 * views re-project. On every other platform it is a plain passthrough `View`.
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
 * On Android 13+ the full AGSL pipeline runs (refraction, dispersion, rim
 * light, grain); Android 12 gets frosted blur, and everything back to Android
 * 10 gets a clipped backdrop with a scrim. On non-Android platforms (iOS, web)
 * the surface is a plain passthrough `View` and the optical props below are
 * ignored — children still render, with no glass effect.
 */
export interface LiquidGlassViewProps extends ViewProps {
  /** Backdrop to refract — must match a provider's `providerId`. */
  providerId?: string;

  /** Corner radius in dp. Omit for a capsule (fully rounded). */
  cornerRadius?: number;

  /** Backdrop blur radius in dp — the frostiness. Default 20. */
  blurRadius?: number;

  /** Width of the refracting band along the rim, in dp. Default 12. */
  refractionHeight?: number;

  /** Peak refraction displacement in dp (negative bends outward). Default 16. */
  refractionAmount?: number;

  /** Backdrop saturation boost; 1 = unchanged. Default 1.5. */
  saturation?: number;

  /** Surface tint; the alpha channel is the blend strength. */
  tint?: ColorValue;

  /** 0..1 RGB dispersion along the lens edge. Default 0. */
  chromaticAberration?: number;

  /** Strength of the anti-banding grain, 0..1. Default 0.015. */
  noiseAlpha?: number;

  /** 0..1 intensity of the specular rim highlight. Default 0.55. */
  highlightAlpha?: number;

  /** Thickness of the specular rim in dp. Default 2.5. */
  highlightWidth?: number;

  /** Screen-space angle the rim light comes from, in degrees. Default 245. */
  lightAngle?: number;

  /** Respond to touch with the gel press (scale, bulge, brighter rim). */
  interactive?: boolean;
}

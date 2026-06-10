import ExpoModulesCore

/**
 Native side of `<LiquidGlassProvider>` on iOS.

 UIKit's visual effect views sample whatever is rendered behind them natively,
 so the provider needs no capture machinery here — it is a plain passthrough
 container kept for cross-platform API symmetry with Android.
 */
public class ExpoLiquidGlassProviderModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoLiquidGlassProvider")

    View(LiquidGlassProviderView.self) {
      Prop("providerId") { (_: LiquidGlassProviderView, _: String) in
        // Only meaningful on Android; accepted so shared JS just works.
      }
    }
  }
}

class LiquidGlassProviderView: ExpoView {}

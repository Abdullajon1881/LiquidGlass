import ExpoModulesCore

/**
 Native side of `<LiquidGlassView>` on iOS.

 On iOS 26+ this hosts Apple's real `UIGlassEffect` inside a
 `UIVisualEffectView`; the system performs its own backdrop sampling,
 refraction and highlighting, so the Android optical tuning props are accepted
 as no-ops. Below iOS 26 it falls back to an ultra-thin material blur.
 */
public class ExpoLiquidGlassModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoLiquidGlass")

    View(LiquidGlassView.self) {
      Prop("cornerRadius") { (view: LiquidGlassView, radius: Double?) in
        view.setCornerRadius(radius)
      }
      Prop("tint") { (view: LiquidGlassView, color: UIColor?) in
        view.setTint(color)
      }
      Prop("interactive") { (view: LiquidGlassView, interactive: Bool?) in
        view.setInteractive(interactive ?? false)
      }

      // Android-only tuning props, accepted so shared JS just works.
      Prop("providerId") { (_: LiquidGlassView, _: String?) in }
      Prop("blurRadius") { (_: LiquidGlassView, _: Double?) in }
      Prop("refractionHeight") { (_: LiquidGlassView, _: Double?) in }
      Prop("refractionAmount") { (_: LiquidGlassView, _: Double?) in }
      Prop("saturation") { (_: LiquidGlassView, _: Double?) in }
      Prop("chromaticAberration") { (_: LiquidGlassView, _: Double?) in }
      Prop("noiseAlpha") { (_: LiquidGlassView, _: Double?) in }
      Prop("highlightAlpha") { (_: LiquidGlassView, _: Double?) in }
      Prop("highlightWidth") { (_: LiquidGlassView, _: Double?) in }
      Prop("lightAngle") { (_: LiquidGlassView, _: Double?) in }
    }
  }
}

class LiquidGlassView: ExpoView {
  private let effectView = UIVisualEffectView(effect: nil)
  private var isInteractiveGlass = false
  private var glassTint: UIColor?
  /// nil = capsule (radius follows bounds); a number = fixed radius in points.
  private var explicitCornerRadius: Double?

  required init(appContext: AppContext? = nil) {
    super.init(appContext: appContext)
    clipsToBounds = true
    effectView.frame = bounds
    effectView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    addSubview(effectView)
    applyEffect()
  }

  override func didAddSubview(_ subview: UIView) {
    super.didAddSubview(subview)
    // React Native children always render above the glass.
    if subview !== effectView {
      sendSubviewToBack(effectView)
    }
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    let radius: CGFloat
    if let explicit = explicitCornerRadius {
      radius = min(CGFloat(explicit), min(bounds.width, bounds.height) / 2)
    } else {
      radius = min(bounds.width, bounds.height) / 2
    }
    layer.cornerRadius = radius
    layer.cornerCurve = .continuous
  }

  func setCornerRadius(_ radius: Double?) {
    explicitCornerRadius = radius
    setNeedsLayout()
  }

  func setTint(_ color: UIColor?) {
    glassTint = color
    applyEffect()
  }

  func setInteractive(_ interactive: Bool) {
    isInteractiveGlass = interactive
    applyEffect()
  }

  private func applyEffect() {
    if #available(iOS 26.0, *) {
      let glass = UIGlassEffect()
      glass.isInteractive = isInteractiveGlass
      if let tint = glassTint {
        glass.tintColor = tint
      }
      effectView.effect = glass
    } else {
      effectView.effect = UIBlurEffect(style: .systemUltraThinMaterial)
      effectView.backgroundColor = glassTint?.withAlphaComponent(
        (glassTint?.cgColor.alpha ?? 0) * 0.4
      )
    }
  }
}

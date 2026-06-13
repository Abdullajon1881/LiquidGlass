Pod::Spec.new do |s|
  s.name           = 'ExpoLiquidGlass'
  s.version        = '1.0.0'
  s.summary        = 'Apple Liquid Glass for React Native and Expo'
  s.description    = 'Real UIGlassEffect on iOS 26+, UIBlurEffect fallback below; AGSL refraction shaders on Android.'
  s.author         = 'LiquidGlass contributors'
  s.homepage       = 'https://github.com/Abdullajon1881/LiquidGlass'
  s.license        = { :type => 'Apache-2.0' }
  s.platforms      = { :ios => '15.1' }
  s.swift_version  = '5.9'
  s.source         = { :git => 'https://github.com/Abdullajon1881/LiquidGlass.git', :tag => "liquid-glass-kit-#{s.version}" }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
  }

  s.source_files = '**/*.swift'
end

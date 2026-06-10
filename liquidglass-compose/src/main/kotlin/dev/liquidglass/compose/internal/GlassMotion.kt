package dev.liquidglass.compose.internal

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * The motion vocabulary of Liquid Glass: quick, slightly viscous on the way in,
 * bouncy and elastic on the way out — the timing that makes glass feel like gel.
 */
internal object GlassMotion {

    /** Peak scale gain of an interactive element while pressed. */
    internal const val PRESS_SCALE_MAX: Float = 0.025f

    /** Scale a container shape morphs from/to when appearing or disappearing. */
    internal const val MORPH_MIN_SCALE: Float = 0.4f

    /** Press-in: fast and nearly critically damped, like sinking into gel. */
    internal val PressIn: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 1200f)

    /** Release: springy overshoot as the glass bounces back. */
    internal val Release: SpringSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 280f)

    /** A shape emerging into a container, with a soft overshoot. */
    internal val Appear: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 380f)

    /** A shape being absorbed back into the container, no bounce. */
    internal val Disappear: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 700f)
}

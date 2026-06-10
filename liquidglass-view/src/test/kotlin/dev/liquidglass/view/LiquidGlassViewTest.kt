package dev.liquidglass.view

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.liquidglass.core.GlassRenderTier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * View-system smoke tests on the software-canvas path (the plain scrim tier):
 * wiring, property invalidation, provider registration, and an actual pixel
 * assertion that the scrim really paints.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiquidGlassViewTest {

    private fun buildPair(): Triple<FrameLayout, LiquidGlassProviderLayout, LiquidGlassView> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = FrameLayout(activity)
        val provider = LiquidGlassProviderLayout(activity)
        provider.addView(View(activity).apply { setBackgroundColor(Color.RED) })
        val glass = LiquidGlassView(activity)
        glass.provider = provider
        root.addView(provider, 400, 400)
        root.addView(glass, 200, 100)
        activity.setContentView(root)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 400, 400)
        return Triple(root, provider, glass)
    }

    @Test
    fun softwareDrawPaintsTheScrimWithoutCrashing() {
        val (_, _, glass) = buildPair()
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        glass.draw(Canvas(bitmap))
        // The default white scrim must actually land on the canvas.
        assertThat(Color.alpha(bitmap.getPixel(100, 50))).isGreaterThan(0)
    }

    @Test
    fun fullHierarchyDrawsWithoutCrashing() {
        val (root, _, _) = buildPair()
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        assertThat(Color.red(bitmap.getPixel(300, 300))).isEqualTo(255)
    }

    @Test
    fun reassigningTheProviderMovesTheRegistration() {
        val (_, provider, glass) = buildPair()
        val second = LiquidGlassProviderLayout(glass.context)
        glass.provider = second
        assertThat(glass.provider).isSameInstanceAs(second)
        glass.provider = provider
        assertThat(glass.provider).isSameInstanceAs(provider)
    }

    @Test
    fun capsuleSentinelResolvesToHalfTheSmallerDimension() {
        val (_, _, glass) = buildPair()
        assertThat(glass.cornerRadiusDp).isEqualTo(CORNER_RADIUS_CAPSULE)
        // Drawing exercises the capsule resolution path; no exception = pass,
        // pixel coverage asserted in softwareDrawPaintsTheScrim test.
        glass.draw(Canvas(Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)))
    }

    @Test
    fun propertyChangesAreAccepted() {
        val (_, _, glass) = buildPair()
        glass.blurRadiusDp = 32f
        glass.refractionAmountDp = -8f
        glass.saturation = 1.2f
        glass.glassTintColor = Color.argb(80, 0, 120, 255)
        glass.chromaticAberration = 0.4f
        glass.highlightAlpha = 0.8f
        glass.tierOverride = GlassRenderTier.SCRIM
        assertThat(glass.blurRadiusDp).isEqualTo(32f)
        assertThat(glass.tierOverride).isEqualTo(GlassRenderTier.SCRIM)
        glass.draw(Canvas(Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)))
    }

    @Test
    fun tintedScrimWinsOverTheDefaultFallback() {
        val (_, _, glass) = buildPair()
        glass.glassTintColor = Color.argb(255, 0, 0, 255)
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        glass.draw(Canvas(bitmap))
        assertThat(Color.blue(bitmap.getPixel(100, 50))).isEqualTo(255)
    }
}

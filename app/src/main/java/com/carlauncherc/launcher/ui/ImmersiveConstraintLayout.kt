package com.carlauncherc.launcher.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Root layout for the launcher dashboard.
 *
 * Many Android head units expose the system navigation/status controls as a vertical rail on
 * the left side. A HOME app should use the complete display, so this view re-applies immersive
 * mode whenever the window regains focus or the vendor SystemUI makes the bars visible again.
 * Swiping from the screen edge can still reveal the system controls temporarily.
 */
class ImmersiveConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyImmersiveMode()

        @Suppress("DEPRECATION")
        setOnSystemUiVisibilityChangeListener {
            postDelayed({ applyImmersiveMode() }, REHIDE_DELAY_MS)
        }
    }

    override fun onDetachedFromWindow() {
        @Suppress("DEPRECATION")
        setOnSystemUiVisibilityChangeListener(null)
        super.onDetachedFromWindow()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) applyImmersiveMode()
    }

    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsetsController?.let { controller ->
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsets.Type.systemBars())
            }
        }

        // Keep the legacy flags as well. A large number of Android 10/11 head-unit ROMs use
        // custom SystemUI code that reacts to these flags even when WindowInsetsController is
        // only partially implemented.
        @Suppress("DEPRECATION")
        systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    private companion object {
        const val REHIDE_DELAY_MS = 250L
    }
}

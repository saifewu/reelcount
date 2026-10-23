package app.reelcount

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import kotlin.math.max

/**
 * The live counter drawn at the camera cutout.
 *
 * Island: a dark pill centred on the punch-hole, dot on one side, count on the other.
 * Beside: a small pill just to the right of the cutout.
 *
 * It's an accessibility overlay, so it can sit over the status bar and needs
 * no "draw over other apps" permission. It ignores touches.
 */
class NotchPill(private val context: Context) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private val font = context.resources.getFont(R.font.poppins_medium)

    private val pillBg = GradientDrawable()
    private val dotBg = GradientDrawable().apply { shape = GradientDrawable.OVAL }
    private var root: View? = null
    private var builtStyle: String? = null
    private lateinit var dot: View
    private lateinit var label: TextView

    fun render(visible: Boolean, count: Int, overLimit: Boolean, style: String) {
        if (!visible) return remove()
        if (root == null || style != builtStyle) {
            remove()
            add(style)
        }
        label.text = count.toString()
        pillBg.setColor(if (overLimit) Brand.ORANGE else Brand.DARK)
        dotBg.setColor(if (overLimit) Brand.IVORY else Brand.ORANGE)
    }

    fun pulse() {
        if (root == null) return
        for ((view, scale) in listOf(dot to 1.8f, label to 1.18f)) {
            view.animate().cancel()
            view.animate().scaleX(scale).scaleY(scale).setDuration(120).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
            }.start()
        }
    }

    fun remove() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }

    private fun add(style: String) {
        val cut = topCutout()
        val h = max(cut.height(), dp(28))
        val dotSize = dp(7)
        pillBg.cornerRadius = h / 2f

        dot = View(context).apply { background = dotBg }
        label = TextView(context).apply {
            typeface = font
            textSize = 13f
            includeFontPadding = false
            gravity = Gravity.CENTER
            setTextColor(Brand.IVORY)
        }
        val pill = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = pillBg
        }

        val width: Int
        val x: Int
        if (style == Prefs.ISLAND) {
            val side = dp(44)
            val dotBox = FrameLayout(context).apply {
                addView(dot, FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER))
            }
            pill.addView(dotBox, LinearLayout.LayoutParams(side, h))
            pill.addView(Space(context), LinearLayout.LayoutParams(cut.width(), h))
            pill.addView(label, LinearLayout.LayoutParams(side, h))
            width = side * 2 + cut.width()
            x = cut.centerX() - width / 2
        } else {
            pill.setPadding(dp(10), 0, dp(12), 0)
            pill.addView(dot, LinearLayout.LayoutParams(dotSize, dotSize).apply { marginEnd = dp(6) })
            pill.addView(label, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            width = WRAP_CONTENT
            x = if (cut.width() > 0) cut.right + dp(8) else cut.centerX() - dp(24)
        }

        val params = WindowManager.LayoutParams(
            width,
            h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            this.x = max(0, x)
            y = max(0, cut.centerY() - h / 2)
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        wm.addView(pill, params)
        root = pill
        builtStyle = style
    }

    /** The camera cutout at the top of the screen, or a zero-width strip at the status bar's centre. */
    @Suppress("DEPRECATION")
    private fun topCutout(): Rect {
        val top = when {
            Build.VERSION.SDK_INT >= 30 -> wm.currentWindowMetrics.windowInsets.displayCutout?.boundingRectTop
            Build.VERSION.SDK_INT >= 29 -> wm.defaultDisplay.cutout?.boundingRectTop
            else -> null
        }
        if (top != null && !top.isEmpty) return top

        val mid = context.resources.displayMetrics.widthPixels / 2
        val barId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val bar = if (barId > 0) context.resources.getDimensionPixelSize(barId) else dp(24)
        return Rect(mid, 0, mid, bar)
    }

    private fun dp(v: Int) = (v * density).toInt()
}

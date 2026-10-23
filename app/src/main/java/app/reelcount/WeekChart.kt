package app.reelcount

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Seven quiet bars; today's is orange. */
class WeekChart(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var days: List<Pair<LocalDate, Int>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = resources.getFont(R.font.poppins_medium)
        textSize = 11 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        if (days.isEmpty()) return
        val labelH = 22 * density
        val valueH = 18 * density
        val chartH = height - labelH - valueH
        val slot = width / days.size.toFloat()
        val barW = min(slot * 0.42f, 22 * density)
        val peak = max(1, days.maxOf { it.second })
        val today = LocalDate.now()

        days.forEachIndexed { i, (day, n) ->
            val cx = slot * i + slot / 2
            val isToday = day == today
            val h = max(3 * density, chartH * n / peak)
            val top = valueH + chartH - h

            bar.color = if (isToday) Brand.ORANGE else Brand.LIGHT
            rect.set(cx - barW / 2, top, cx + barW / 2, valueH + chartH)
            canvas.drawRoundRect(rect, 6 * density, 6 * density, bar)

            if (n > 0) {
                text.color = if (isToday) Brand.DARK else Brand.MID
                canvas.drawText(n.toString(), cx, top - 6 * density, text)
            }
            text.color = if (isToday) Brand.DARK else Brand.MUTED
            canvas.drawText(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), cx, height - 4 * density, text)
        }
    }
}

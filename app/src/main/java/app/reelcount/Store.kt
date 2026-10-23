package app.reelcount

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/** Anthropic brand palette, for views drawn in code. */
object Brand {
    const val DARK = 0xFF141413.toInt()
    const val IVORY = 0xFFFAF9F5.toInt()
    const val ORANGE = 0xFFD97757.toInt()
    const val LIGHT = 0xFFE8E6DC.toInt()
    const val MID = 0xFFB0AEA5.toInt()
    const val MUTED = 0xFF73726C.toInt()
}

/** Reel counts, one entry per day, so "today" rolls over at midnight on its own. */
object Stats {
    const val DEBUG = "debug"
    private const val TOTAL = "total"

    fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("stats", Context.MODE_PRIVATE)

    private fun dayKey(day: LocalDate) = "d_$day"

    fun today(c: Context) = prefs(c).getInt(dayKey(LocalDate.now()), 0)

    fun total(c: Context) = prefs(c).getInt(TOTAL, 0)

    fun add(c: Context, n: Int) {
        val p = prefs(c)
        val key = dayKey(LocalDate.now())
        p.edit()
            .putInt(key, p.getInt(key, 0) + n)
            .putInt(TOTAL, p.getInt(TOTAL, 0) + n)
            .apply()
    }

    fun resetToday(c: Context) {
        val p = prefs(c)
        val key = dayKey(LocalDate.now())
        p.edit()
            .putInt(TOTAL, maxOf(0, p.getInt(TOTAL, 0) - p.getInt(key, 0)))
            .remove(key)
            .apply()
    }

    /** The last [n] days, oldest first, ending today. */
    fun lastDays(c: Context, n: Int = 7): List<Pair<LocalDate, Int>> {
        val p = prefs(c)
        val today = LocalDate.now()
        return (n - 1 downTo 0).map { ago ->
            val day = today.minusDays(ago.toLong())
            day to p.getInt(dayKey(day), 0)
        }
    }

    fun debug(c: Context, line: String) = prefs(c).edit().putString(DEBUG, line).apply()
}

object Prefs {
    const val STYLE = "style"
    const val SHOW = "show"
    const val LIMIT = "limit"

    const val ISLAND = "island"
    const val BESIDE = "beside"
    const val FACEBOOK = "facebook"
    const val ALWAYS = "always"

    fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun style(c: Context) = prefs(c).getString(STYLE, ISLAND)!!
    fun show(c: Context) = prefs(c).getString(SHOW, FACEBOOK)!!
    fun limit(c: Context) = prefs(c).getInt(LIMIT, 0)

    fun put(c: Context, key: String, value: String) = prefs(c).edit().putString(key, value).apply()
    fun put(c: Context, key: String, value: Int) = prefs(c).edit().putInt(key, value).apply()
}

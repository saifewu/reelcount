package app.reelcount

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import java.text.NumberFormat

class MainActivity : Activity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val number = NumberFormat.getIntegerInstance()

    private lateinit var count: TextView
    private lateinit var subtitle: TextView
    private lateinit var week: WeekChart
    private lateinit var statusOn: View
    private lateinit var statusOff: View
    private lateinit var segIsland: TextView
    private lateinit var segBeside: TextView
    private lateinit var segFacebook: TextView
    private lateinit var segAlways: TextView
    private lateinit var limitValue: TextView
    private lateinit var reset: TextView
    private lateinit var debug: TextView

    private var resetArmed = false
    private val disarmReset = Runnable {
        resetArmed = false
        reset.setText(R.string.reset_today)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        count = findViewById(R.id.count)
        subtitle = findViewById(R.id.subtitle)
        week = findViewById(R.id.week)
        statusOn = findViewById(R.id.status_on)
        statusOff = findViewById(R.id.status_off)
        segIsland = findViewById(R.id.seg_island)
        segBeside = findViewById(R.id.seg_beside)
        segFacebook = findViewById(R.id.seg_facebook)
        segAlways = findViewById(R.id.seg_always)
        limitValue = findViewById(R.id.limit_value)
        reset = findViewById(R.id.reset)
        debug = findViewById(R.id.debug)

        segIsland.setOnClickListener { Prefs.put(this, Prefs.STYLE, Prefs.ISLAND) }
        segBeside.setOnClickListener { Prefs.put(this, Prefs.STYLE, Prefs.BESIDE) }
        segFacebook.setOnClickListener { Prefs.put(this, Prefs.SHOW, Prefs.FACEBOOK) }
        segAlways.setOnClickListener { Prefs.put(this, Prefs.SHOW, Prefs.ALWAYS) }
        findViewById<View>(R.id.limit_minus).setOnClickListener {
            Prefs.put(this, Prefs.LIMIT, maxOf(0, Prefs.limit(this) - LIMIT_STEP))
        }
        findViewById<View>(R.id.limit_plus).setOnClickListener {
            Prefs.put(this, Prefs.LIMIT, minOf(500, Prefs.limit(this) + LIMIT_STEP))
        }
        findViewById<View>(R.id.open_a11y).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<View>(R.id.open_app_info).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
        reset.setOnClickListener {
            reset.removeCallbacks(disarmReset)
            if (resetArmed) {
                Stats.resetToday(this)
                disarmReset.run()
            } else {
                resetArmed = true
                reset.setText(R.string.reset_confirm)
                reset.postDelayed(disarmReset, 3000)
            }
        }
        // Long-press the number to see what the detector last decided.
        count.setOnLongClickListener {
            debug.visibility = if (debug.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            true
        }
    }

    override fun onResume() {
        super.onResume()
        Stats.prefs(this).registerOnSharedPreferenceChangeListener(this)
        Prefs.prefs(this).registerOnSharedPreferenceChangeListener(this)
        render()
    }

    override fun onPause() {
        Stats.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        Prefs.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) = render()

    private fun render() {
        val today = Stats.today(this)
        val limit = Prefs.limit(this)

        count.text = number.format(today)
        count.setTextColor(if (limit in 1..today) Brand.ORANGE else Brand.DARK)
        subtitle.text = buildString {
            append(getString(R.string.all_time, number.format(Stats.total(this@MainActivity))))
            if (limit > 0) append(" · ").append(getString(R.string.limit_of, number.format(limit)))
        }
        week.days = Stats.lastDays(this)

        val enabled = isCounterEnabled()
        statusOn.visibility = if (enabled) View.VISIBLE else View.GONE
        statusOff.visibility = if (enabled) View.GONE else View.VISIBLE

        select(Prefs.style(this) == Prefs.ISLAND, segIsland, segBeside)
        select(Prefs.show(this) == Prefs.FACEBOOK, segFacebook, segAlways)
        limitValue.text = if (limit == 0) getString(R.string.off) else number.format(limit)
        debug.text = Stats.prefs(this).getString(Stats.DEBUG, null) ?: getString(R.string.no_scrolls)
    }

    private fun select(firstOn: Boolean, first: TextView, second: TextView) {
        for ((view, on) in listOf(first to firstOn, second to !firstOn)) {
            view.setBackgroundResource(if (on) R.drawable.bg_segment_on else 0)
            view.setTextColor(if (on) Brand.IVORY else Brand.DARK)
        }
    }

    private fun isCounterEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(this, ReelCounterService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private companion object {
        const val LIMIT_STEP = 10
    }
}

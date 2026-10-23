package app.reelcount

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.max

/**
 * Watches Facebook for swipes in the full-screen Reels pager and counts each
 * new reel that settles on screen.
 *
 * Facebook scrambles its view ids, so detection goes by shape instead: a
 * scrolling container that fills (nearly) the whole screen, with one child
 * that fills the container. The feed never looks like that; Reels always does.
 */
class ReelCounterService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val handler = Handler(Looper.getMainLooper())
    private val settle = Runnable { onScrollSettled() }
    private lateinit var pill: NotchPill

    private var facebookInFront = false
    private var pendingNode: AccessibilityNodeInfo? = null
    private var pendingFrom = -1
    private var pendingTo = -1

    // Per Facebook visit: reels already counted, so swiping back doesn't recount.
    private val seen = HashSet<String>()
    private var lastKey: String? = null
    private var countedStart = false

    override fun onServiceConnected() {
        pill = NotchPill(this)
        Stats.prefs(this).registerOnSharedPreferenceChangeListener(this)
        Prefs.prefs(this).registerOnSharedPreferenceChangeListener(this)
        renderPill()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindowChanged(pkg)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> if (pkg == FACEBOOK) onScrolled(event)
        }
    }

    private fun onWindowChanged(pkg: String) {
        // The notification shade and keyboards pop up over Facebook without leaving it.
        if (pkg == packageName || pkg == "com.android.systemui" || "inputmethod" in pkg || "keyboard" in pkg) return
        val inFront = pkg == FACEBOOK
        if (inFront == facebookInFront) return
        facebookInFront = inFront
        if (!inFront) {
            handler.removeCallbacks(settle)
            pendingNode = null
            seen.clear()
            lastKey = null
            countedStart = false
        }
        renderPill()
    }

    private fun onScrolled(event: AccessibilityEvent) {
        if (!facebookInFront) {
            facebookInFront = true
            renderPill()
        }
        val source = event.source ?: return
        pendingNode = source
        pendingFrom = event.fromIndex
        pendingTo = event.toIndex
        // Judge the reel only once the swipe has snapped into place.
        handler.removeCallbacks(settle)
        handler.postDelayed(settle, SETTLE_MS)
    }

    private fun onScrollSettled() {
        val node = pendingNode ?: return
        pendingNode = null
        if (!node.refresh()) return

        val box = Rect().also(node::getBoundsInScreen)
        val screen = screenSize()
        val containerPct = box.height() * 100 / max(1, screen.y)
        val itemPct = largestVisibleChildHeight(node) * 100 / max(1, box.height())
        val note = "container $containerPct% · item $itemPct% · index $pendingFrom–$pendingTo"

        val isReelPager = containerPct >= MIN_CONTAINER_PCT &&
            box.width() >= screen.x * 9 / 10 &&
            itemPct >= MIN_ITEM_PCT
        if (!isReelPager) return Stats.debug(this, "$note · not Reels")

        val key = reelKey(node) ?: return Stats.debug(this, "$note · no reel id")
        if (key == lastKey) return
        lastKey = key
        if (!seen.add(key)) return Stats.debug(this, "$note · already counted")

        // The reel Reels opened on never scrolls in, so it's counted with the first swipe.
        val added = if (countedStart) 1 else 2
        countedStart = true
        Stats.add(this, added)
        Stats.debug(this, "$note · +$added")
        pill.pulse()
    }

    /** Pager position when Facebook reports one, otherwise the reel's visible text. */
    private fun reelKey(pager: AccessibilityNodeInfo): String? {
        val container = "${pager.windowId}:${pager.hashCode()}"
        if (pendingFrom >= 0 && pendingFrom == pendingTo) return "$container#$pendingFrom"
        return visibleText(pager)?.let { "$container~$it" }
    }

    private fun visibleText(root: AccessibilityNodeInfo): String? {
        val parts = ArrayList<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited < 120 && parts.size < 12) {
            val n = queue.removeFirst()
            visited++
            if (n !== root && !n.isVisibleToUser) continue
            for (s in arrayOf(n.text, n.contentDescription)) {
                val t = s?.toString()?.trim() ?: continue
                // Skip like/comment counts, which tick up while you watch.
                if (t.length in 3..80 && t.none(Char::isDigit)) parts += t
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let(queue::add)
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("|")
    }

    private fun largestVisibleChildHeight(node: AccessibilityNodeInfo): Int {
        val r = Rect()
        var best = 0
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (!child.isVisibleToUser) continue
            child.getBoundsInScreen(r)
            best = max(best, r.height())
        }
        return best
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): Point {
        val wm = getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.currentWindowMetrics.bounds
            return Point(b.width(), b.height())
        }
        val dm = DisplayMetrics().also(wm.defaultDisplay::getRealMetrics)
        return Point(dm.widthPixels, dm.heightPixels)
    }

    private fun renderPill() {
        if (!::pill.isInitialized) return
        val today = Stats.today(this)
        val limit = Prefs.limit(this)
        pill.render(
            visible = facebookInFront || Prefs.show(this) == Prefs.ALWAYS,
            count = today,
            overLimit = limit in 1..today,
            style = Prefs.style(this),
        )
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key != Stats.DEBUG) renderPill()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!::pill.isInitialized) return
        pill.remove()
        renderPill()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        Stats.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        Prefs.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        if (::pill.isInitialized) pill.remove()
        super.onDestroy()
    }

    private companion object {
        const val FACEBOOK = "com.facebook.katana"
        const val SETTLE_MS = 450L
        const val MIN_CONTAINER_PCT = 85
        const val MIN_ITEM_PCT = 90
    }
}

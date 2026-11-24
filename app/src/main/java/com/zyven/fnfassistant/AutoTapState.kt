package com.zyven.fnfassistant

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.view.Display

/** Shared auto-tap settings and status. Auto-tap is never persisted as "on": it always starts OFF. */
object AutoTapState {
    const val HIT_LINE_MIN = 0.05f
    const val HIT_LINE_MAX = 0.40f
    const val LEAD_MIN = 0
    const val LEAD_MAX = 250

    @Volatile var enabled: Boolean = false
    @Volatile var serviceConnected: Boolean = false

    /** Vertical position of the receptor line, as a fraction of the screen height. */
    @Volatile var hitLineY: Float = 0.115f

    /** How many milliseconds before the note reaches the line the tap is sent. */
    @Volatile var leadMs: Int = 90

    @Volatile var showGuide: Boolean = true
    @Volatile var tapCount: Int = 0
    @Volatile var warning: String = ""

    fun load(prefs: SharedPreferences) {
        hitLineY = prefs.getFloat("hit_line_y", 0.115f).coerceIn(HIT_LINE_MIN, HIT_LINE_MAX)
        leadMs = prefs.getInt("lead_ms", 90).coerceIn(LEAD_MIN, LEAD_MAX)
        showGuide = prefs.getBoolean("show_guide", true)
    }

    fun save(prefs: SharedPreferences) {
        prefs.edit()
            .putFloat("hit_line_y", hitLineY)
            .putInt("lead_ms", leadMs)
            .putBoolean("show_guide", showGuide)
            .apply()
    }
}

/** Physical screen size in the current rotation (what screen capture and gestures both use). */
@Suppress("DEPRECATION")
internal fun realScreenSize(context: Context): Point {
    val point = Point()
    context.getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)
        ?.getRealSize(point)
    return point
}

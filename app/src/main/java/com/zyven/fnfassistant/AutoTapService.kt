package com.zyven.fnfassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

object AutoTapState {
    @Volatile var enabled: Boolean = false
    @Volatile var serviceConnected: Boolean = false
    private val lastTapAt = LongArray(4)

    @Synchronized
    fun shouldTap(lane: Int, now: Long): Boolean {
        if (lane !in 1..4 || now - lastTapAt[lane - 1] < 135L) return false
        lastTapAt[lane - 1] = now
        return true
    }
}

class AutoTapService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AutoTapState.serviceConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        AutoTapState.enabled = false
    }

    override fun onDestroy() {
        AutoTapState.enabled = false
        AutoTapState.serviceConnected = false
        instance = null
        super.onDestroy()
    }

    fun tapLane(lane: Int) {
        if (!AutoTapState.enabled || lane !in 1..4) return
        val metrics = resources.displayMetrics
        val centers = floatArrayOf(0.2875f, 0.4125f, 0.6000f, 0.7250f)
        val x = centers[lane - 1] * metrics.widthPixels
        val y = metrics.heightPixels * 0.8333f
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 35L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    companion object {
        @Volatile var instance: AutoTapService? = null
            private set
    }
}

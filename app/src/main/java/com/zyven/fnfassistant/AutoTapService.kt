package com.zyven.fnfassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

object AutoTapState {
    @Volatile var enabled: Boolean = false
    @Volatile var serviceConnected: Boolean = false
    private val insideHitZone = BooleanArray(4)

    @Synchronized
    fun newlyReachedHitZone(candidates: List<NoteCandidate>): List<Int> {
        val near = BooleanArray(4)
        candidates.filter { it.lane in 1..4 && it.centerY >= 0.78f }
            .forEach { near[it.lane - 1] = true }
        val taps = ArrayList<Int>(4)
        for (index in 0..3) {
            if (near[index] && !insideHitZone[index]) taps.add(index + 1)
            insideHitZone[index] = near[index]
        }
        return taps
    }

    @Synchronized
    fun resetHitZone() {
        insideHitZone.fill(false)
    }
}

class AutoTapService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AutoTapState.serviceConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { AutoTapState.enabled = false }

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

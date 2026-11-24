package com.zyven.fnfassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

/**
 * Sends short taps on the four FNF lanes. It only does anything while auto-tap is switched on
 * from the floating menu. It does not read window content and ignores all accessibility events.
 */
class AutoTapService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AutoTapState.serviceConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty: events are not used.
    }

    override fun onInterrupt() {
        // Nothing to interrupt: taps are one-shot gestures.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        if (instance === this) instance = null
        AutoTapState.serviceConnected = false
        AutoTapState.enabled = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun schedule(plans: List<TapPlanner.Plan>) {
        if (!AutoTapState.enabled) return
        if (!screenMatchesCapture()) {
            AutoTapState.warning =
                "Yakalama yönü ekranla uyuşmuyor. Oyunu yatay aç, ekran yakalamayı yeniden başlat."
            return
        }
        val sorted = plans.sortedBy { it.delayMs }
        var i = 0
        while (i < sorted.size) {
            val first = sorted[i]
            val lanes = ArrayList<Int>()
            lanes.add(first.lane)
            var j = i + 1
            // Notes due at (almost) the same time go out as one multi-finger gesture.
            while (j < sorted.size && sorted[j].delayMs - first.delayMs <= CHORD_WINDOW_MS) {
                lanes.add(sorted[j].lane)
                j++
            }
            handler.postDelayed({ tapLanes(lanes, force = false) }, first.delayMs)
            i = j
        }
    }

    private fun runTest() {
        for (lane in 1..4) {
            handler.postDelayed({ tapLanes(listOf(lane), force = true) }, (lane - 1) * 350L)
        }
    }

    private fun tapLanes(lanes: List<Int>, force: Boolean) {
        if (!force && !AutoTapState.enabled) return
        val size = realScreenSize(this)
        if (size.x <= 0 || size.y <= 0) return
        val y = AutoTapState.hitLineY * size.y
        val builder = GestureDescription.Builder()
        var strokes = 0
        for (lane in lanes.distinct()) {
            val index = lane - 1
            if (index !in NoteDetector.laneCenters.indices) continue
            if (strokes >= GestureDescription.getMaxStrokeCount()) break
            val x = NoteDetector.laneCenters[index] * size.x
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x + 1f, y)
            }
            builder.addStroke(GestureDescription.StrokeDescription(path, 0L, TAP_MS))
            strokes++
        }
        if (strokes == 0) return
        if (dispatchGesture(builder.build(), null, null)) AutoTapState.tapCount += strokes
    }

    private fun screenMatchesCapture(): Boolean {
        val frameW = DetectionState.frameWidth
        val frameH = DetectionState.frameHeight
        if (frameW <= 0 || frameH <= 0) return false
        val size = realScreenSize(this)
        if (size.x <= 0 || size.y <= 0) return false
        val frameRatio = frameW.toFloat() / frameH
        val screenRatio = size.x.toFloat() / size.y
        return abs(frameRatio - screenRatio) / screenRatio < 0.05f
    }

    companion object {
        private const val TAP_MS = 30L
        private const val CHORD_WINDOW_MS = 20L

        @Volatile
        private var instance: AutoTapService? = null

        /** Called from the analysis thread with the taps planned for the latest frame. */
        fun enqueue(plans: List<TapPlanner.Plan>) {
            instance?.schedule(plans)
        }

        /** Taps lanes 1..4 once, 350 ms apart, even while auto-tap is off. */
        fun testTaps() {
            instance?.runTest()
        }
    }
}

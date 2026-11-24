package com.zyven.fnfassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class TapAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    fun tapNormalized(xFraction: Float, yFraction: Float) {
        val dm = resources.displayMetrics
        val path = Path().apply {
            moveTo(dm.widthPixels * xFraction.coerceIn(0f, 1f), dm.heightPixels * yFraction.coerceIn(0f, 1f))
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 65)).build()
        dispatchGesture(gesture, null, null)
    }
    companion object {
        @Volatile var instance: TapAccessibilityService? = null
            private set
    }
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
}

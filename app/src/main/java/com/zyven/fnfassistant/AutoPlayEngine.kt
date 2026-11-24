package com.zyven.fnfassistant

import android.media.Image

object AutoPlayEngine {
    @Volatile var enabled = false
    @Volatile var hitLine = 0.80f
    @Volatile var sensitivity = 70
    @Volatile var lane1 = 0.32f
    @Volatile var lane2 = 0.44f
    @Volatile var lane3 = 0.56f
    @Volatile var lane4 = 0.68f
    private val lastTap = LongArray(4)
    private const val COOLDOWN_MS = 170L

    fun process(image: Image) {
        if (!enabled) return
        val plane = image.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val w = image.width
        val h = image.height
        val lanes = floatArrayOf(lane1, lane2, lane3, lane4)
        val centerY = (h * hitLine).toInt().coerceIn(2, h - 3)
        val yStart = (centerY - h * 0.025f).toInt().coerceAtLeast(0)
        val yEnd = (centerY + h * 0.025f).toInt().coerceAtMost(h - 1)
        val now = System.currentTimeMillis()
        lanes.forEachIndexed { lane, fraction ->
            if (now - lastTap[lane] < COOLDOWN_MS) return@forEachIndexed
            val cx = (w * fraction).toInt().coerceIn(2, w - 3)
            var hits = 0
            for (y in yStart..yEnd step 2) {
                val left = (cx - w * 0.018f).toInt().coerceAtLeast(0)
                val right = (cx + w * 0.018f).toInt().coerceAtMost(w - 1)
                for (x in left..right step 2) {
                    val i = y * rowStride + x * pixelStride
                    if (i + 2 >= buffer.limit()) continue
                    val r = buffer.get(i).toInt() and 255
                    val g = buffer.get(i + 1).toInt() and 255
                    val b = buffer.get(i + 2).toInt() and 255
                    val high = maxOf(r, g, b)
                    val low = minOf(r, g, b)
                    if (high >= sensitivity && high - low >= 35) hits++
                }
            }
            if (hits >= 5) {
                TapAccessibilityService.instance?.let {
                    it.tapNormalized(fraction, hitLine)
                    lastTap[lane] = now
                }
            }
        }
    }
}

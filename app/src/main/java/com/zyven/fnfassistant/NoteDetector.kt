package com.zyven.fnfassistant

import android.media.Image
import android.os.SystemClock
import kotlin.math.max

data class NoteCandidate(
    val lane: Int,
    val centerY: Float,
    val strength: Int
)

object DetectionState {
    @Volatile var active: Boolean = false
    @Volatile var summary: String = "Ekran yakalama başlatılmadı"
    @Volatile var candidates: List<NoteCandidate> = emptyList()
}

object NoteDetector {
    private const val SAMPLE_STEP = 3
    private const val Y_BUCKET_SIZE = 36
    private var lastAnalysisMs = 0L

    @Synchronized
    fun analyze(image: Image) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalysisMs < 140L) return
        lastAnalysisMs = now

        val plane = image.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val left = (width * 0.06f).toInt()
        val right = (width * 0.94f).toInt()
        val top = (height * 0.16f).toInt()
        val bottom = (height * 0.86f).toInt()
        val laneWidth = max(1, (right - left) / 4)
        val bucketCount = max(1, (bottom - top + Y_BUCKET_SIZE - 1) / Y_BUCKET_SIZE)
        val counts = Array(4) { IntArray(bucketCount) }
        val hues = Array(4) { IntArray(bucketCount) }

        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val lane = ((x - left) / laneWidth).coerceIn(0, 3)
                val bucket = ((y - top) / Y_BUCKET_SIZE).coerceIn(0, bucketCount - 1)
                val index = y * rowStride + x * pixelStride
                if (index >= 0 && index + 2 < buffer.limit()) {
                    val r = buffer.get(index).toInt() and 0xFF
                    val g = buffer.get(index + 1).toInt() and 0xFF
                    val b = buffer.get(index + 2).toInt() and 0xFF
                    val high = max(r, max(g, b))
                    val low = minOf(r, g, b)
                    val saturation = if (high == 0) 0 else (high - low) * 100 / high
                    // Ignore gray UI elements and dark background pixels.
                    if (high > 105 && saturation > 48) {
                        counts[lane][bucket]++
                        hues[lane][bucket] += when {
                            r > g * 1.28 && r > b * 1.22 -> 1 // red
                            g > r * 1.18 && g > b * 1.08 -> 2 // green
                            b > r * 1.15 && b > g * 1.04 -> 3 // blue
                            r > b * 1.08 && b > g * 1.05 -> 4 // purple
                            else -> 0
                        }
                    }
                }
                x += SAMPLE_STEP
            }
            y += SAMPLE_STEP
        }

        val found = ArrayList<NoteCandidate>()
        for (lane in 0..3) {
            var bucket = 0
            while (bucket < bucketCount) {
                if (counts[lane][bucket] < 16) {
                    bucket++
                    continue
                }
                val start = bucket
                var strength = 0
                while (bucket < bucketCount && counts[lane][bucket] >= 8) {
                    strength += counts[lane][bucket]
                    bucket++
                }
                if (strength >= 22) {
                    val center = (top + ((start + bucket - 1) * Y_BUCKET_SIZE / 2f)) / height.toFloat()
                    found.add(NoteCandidate(lane + 1, center, strength))
                }
            }
        }

        DetectionState.candidates = found
        DetectionState.active = true
        val laneSummary = (1..4).map { lane ->
            val amount = found.count { it.lane == lane }
            "Şerit $lane: $amount"
        }.joinToString("  ")
        DetectionState.summary = if (found.isEmpty()) {
            "Ekran analiz ediliyor • nota adayı yok\n$laneSummary"
        } else {
            "${found.size} renkli nota adayı bulundu\n$laneSummary"
        }
    }
}

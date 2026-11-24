package com.zyven.fnfassistant

import android.media.Image
import android.os.SystemClock
import kotlin.math.max
import kotlin.math.min

/**
 * One detected note. [centerY] is the box center; [topY] / [bottomY] are the box edges
 * (all as a fraction of the screen height). Hold notes are tall boxes.
 */
data class NoteCandidate(
    val lane: Int,
    val centerY: Float,
    val strength: Int,
    val topY: Float = centerY,
    val bottomY: Float = centerY
)

object DetectionState {
    @Volatile var active: Boolean = false
    @Volatile var summary: String = "Ekran yakalama başlatılmadı"
    @Volatile var candidates: List<NoteCandidate> = emptyList()
    @Volatile var frameWidth: Int = 0
    @Volatile var frameHeight: Int = 0
}

object NoteDetector {
    private const val STEP = 3
    private const val MIN_COMPONENT = 65
    private const val MAX_COMPONENT = 6500
    private const val ANALYSIS_INTERVAL_MS = 50L

    // Calibrated from the uploaded V-Slice gameplay recording (1600x720).
    // These are the four player-side receptor centers, normalized to screen width.
    val laneCenters = floatArrayOf(0.2875f, 0.4125f, 0.6000f, 0.7250f)
    private var lastAnalysisMs = 0L

    // Reused between frames to avoid allocating about 0.7 MB for every analysis.
    private var maskBuf = BooleanArray(0)
    private var visitedBuf = BooleanArray(0)
    private var queueBuf = IntArray(0)

    /** Returns true when the frame was analyzed, false when it was skipped (too soon / empty). */
    @Synchronized
    fun analyze(image: Image): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalysisMs < ANALYSIS_INTERVAL_MS) return false
        lastAnalysisMs = now

        val plane = image.planes.firstOrNull() ?: return false
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        DetectionState.frameWidth = width
        DetectionState.frameHeight = height

        val gridW = (width + STEP - 1) / STEP
        val gridH = (height + STEP - 1) / STEP
        val cells = gridW * gridH
        if (maskBuf.size != cells) {
            maskBuf = BooleanArray(cells)
            visitedBuf = BooleanArray(cells)
            queueBuf = IntArray(cells.coerceAtMost(120000))
        } else {
            maskBuf.fill(false)
            visitedBuf.fill(false)
        }
        val mask = maskBuf
        val visited = visitedBuf
        val queue = queueBuf

        // Saturated, bright note colors only. Gray receptors and most HUD text are ignored.
        for (gy in 0 until gridH) {
            val y = gy * STEP
            for (gx in 0 until gridW) {
                val x = gx * STEP
                val index = y * rowStride + x * pixelStride
                if (index < 0 || index + 2 >= buffer.limit()) continue
                val r = buffer.get(index).toInt() and 0xFF
                val g = buffer.get(index + 1).toInt() and 0xFF
                val b = buffer.get(index + 2).toInt() and 0xFF
                val high = max(r, max(g, b))
                val low = min(r, min(g, b))
                val saturation = if (high == 0) 0 else (high - low) * 100 / high
                mask[gy * gridW + gx] = high >= 118 && saturation >= 52
            }
        }

        val found = ArrayList<NoteCandidate>()

        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            var minX = gridW
            var maxX = 0
            var minY = gridH
            var maxY = 0
            var area = 0

            while (head < tail) {
                val index = queue[head++]
                val x = index % gridW
                val y = index / gridW
                area++
                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx !in 0 until gridW || ny !in 0 until gridH) continue
                        val ni = ny * gridW + nx
                        if (mask[ni] && !visited[ni]) {
                            visited[ni] = true
                            if (tail < queue.size) queue[tail++] = ni
                        }
                    }
                }
            }

            if (area !in MIN_COMPONENT..MAX_COMPONENT) continue
            val cx = ((minX + maxX) * 0.5f * STEP) / width.toFloat()
            val cy = ((minY + maxY) * 0.5f * STEP) / height.toFloat()
            val boxW = (maxX - minX + 1) * STEP / width.toFloat()
            val boxH = (maxY - minY + 1) * STEP / height.toFloat()

            // Match the player's four receptor columns, not four equal screen quarters.
            val laneIndex = laneCenters.indices.minByOrNull { abs(laneCenters[it] - cx) } ?: continue
            if (abs(laneCenters[laneIndex] - cx) > 0.065f) continue
            if (cy < 0.10f || cy > 0.84f) continue

            // Arrow heads are compact; sustain notes are tall and narrow.
            val arrowLike = boxW in 0.025f..0.115f && boxH in 0.045f..0.22f
            val sustainLike = boxW in 0.018f..0.085f && boxH in 0.20f..0.55f
            if (!arrowLike && !sustainLike) continue

            val topY = minY * STEP / height.toFloat()
            val bottomY = (maxY + 1) * STEP / height.toFloat()
            found.add(NoteCandidate(laneIndex + 1, cy, area, topY, bottomY))
        }

        // Merge multiple color fragments from one note in the same lane and nearby height.
        val merged = ArrayList<NoteCandidate>()
        for (candidate in found.sortedBy { it.centerY }) {
            val previous = merged.lastOrNull { it.lane == candidate.lane && abs(it.centerY - candidate.centerY) < 0.09f }
            if (previous == null) merged.add(candidate)
        }

        DetectionState.candidates = merged
        DetectionState.active = true
        val laneSummary = (1..4).map { lane ->
            "Şerit $lane: ${merged.count { it.lane == lane }}"
        }.joinToString("  ")
        DetectionState.summary = if (merged.isEmpty()) {
            "Oyuncu nota alanı taranıyor • nota bulunamadı\n$laneSummary"
        } else {
            "${merged.size} nota adayı • oyuncu alanı\n$laneSummary"
        }
        return true
    }

    private fun abs(value: Float): Float = kotlin.math.abs(value)
}

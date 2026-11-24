package com.zyven.fnfassistant

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pure logic, no Android classes: turns per-frame note detections into tap times.
 *
 * Every detected note is followed from frame to frame inside its lane. Once its speed is
 * known, the moment its head reaches the hit line is extrapolated and a tap is planned
 * [leadMs] milliseconds before that moment (to cover capture and gesture latency).
 * Each note is planned exactly once.
 */
class TapPlanner {
    /** Tap [lane] (1..4) [delayMs] milliseconds from the frame time passed to [update]. */
    data class Plan(val lane: Int, val delayMs: Long)

    private class Track(val lane: Int, var y: Float, var timeMs: Long, var done: Boolean) {
        var vy = 0f // normalized screen height per millisecond
        var seen = 1
        var lastSeenMs = timeMs
    }

    private val tracks = ArrayList<Track>()
    private val lastTapAtMs = LongArray(LANES + 1) { Long.MIN_VALUE / 2 }

    fun reset() {
        tracks.clear()
        lastTapAtMs.fill(Long.MIN_VALUE / 2)
    }

    fun update(notes: List<NoteCandidate>, nowMs: Long, hitY: Float, leadMs: Long): List<Plan> {
        tracks.removeAll { nowMs - it.lastSeenMs > TRACK_TIMEOUT_MS }

        val plans = ArrayList<Plan>()
        // Notes closest to the line first, so they claim their own tracks first.
        val ordered = notes.sortedBy { abs(headY(it, hitY) - hitY) }
        for (note in ordered) {
            val spanning = isSpanning(note, hitY)
            val ref = headY(note, hitY)

            var best: Track? = null
            var bestDist = Float.MAX_VALUE
            for (track in tracks) {
                if (track.lane != note.lane) continue
                val dt = nowMs - track.timeMs
                if (dt <= 0L) continue // created or already matched in this frame
                val predicted = track.y + track.vy * dt
                val limit = if (track.seen >= 2) MATCH_TIGHT else MATCH_LOOSE
                val dist = abs(ref - predicted)
                if (dist <= limit && dist < bestDist) {
                    best = track
                    bestDist = dist
                }
            }

            if (best == null) {
                tracks.add(Track(note.lane, ref, nowMs, spanning))
                continue
            }

            val dt = (nowMs - best.timeMs).toFloat()
            val instant = (ref - best.y) / dt
            best.vy = if (best.seen >= 2) 0.6f * best.vy + 0.4f * instant else instant
            best.y = ref
            best.timeMs = nowMs
            best.lastSeenMs = nowMs
            best.seen++
            if (spanning) best.done = true
            decide(best, nowMs, hitY, leadMs, plans)
        }
        return plans
    }

    private fun decide(track: Track, nowMs: Long, hitY: Float, leadMs: Long, plans: MutableList<Plan>) {
        if (track.done || track.seen < MIN_SEEN) return
        val speed = abs(track.vy)
        if (speed < MIN_SPEED) return

        val toLine = hitY - track.y
        if (toLine * track.vy <= 0f) {
            // Moving away from the line (or already on it): this note has passed.
            track.done = true
            return
        }

        val msToLine = abs(toLine) / speed
        if (msToLine > leadMs + LOOKAHEAD_MS) return

        track.done = true
        val delay = (msToLine - leadMs).coerceAtLeast(0f).roundToLong()
        val tapAt = nowMs + delay
        if (tapAt - lastTapAtMs[track.lane] < MIN_LANE_GAP_MS) return
        lastTapAtMs[track.lane] = tapAt
        plans.add(Plan(track.lane, delay))
    }

    private fun isTall(note: NoteCandidate) = note.bottomY - note.topY >= TALL_HEIGHT

    /** A hold note whose body already covers the hit line: it is being held, never tap it again. */
    private fun isSpanning(note: NoteCandidate, hitY: Float) =
        isTall(note) && note.topY < hitY - SPAN_MARGIN && note.bottomY > hitY + SPAN_MARGIN

    /** Position of the note head: the center for arrows, the leading end for tall hold notes. */
    private fun headY(note: NoteCandidate, hitY: Float): Float {
        if (!isTall(note)) return note.centerY
        return when {
            note.topY >= hitY - SPAN_MARGIN -> note.topY + HEAD_HALF
            note.bottomY <= hitY + SPAN_MARGIN -> note.bottomY - HEAD_HALF
            else -> hitY
        }
    }

    private companion object {
        const val LANES = 4
        const val TRACK_TIMEOUT_MS = 220L
        const val MATCH_LOOSE = 0.13f
        const val MATCH_TIGHT = 0.07f
        const val MIN_SEEN = 3
        const val MIN_SPEED = 0.0003f // 0.3 screen heights per second
        const val LOOKAHEAD_MS = 70L
        const val MIN_LANE_GAP_MS = 45L
        const val TALL_HEIGHT = 0.20f
        const val SPAN_MARGIN = 0.02f
        const val HEAD_HALF = 0.06f
    }
}

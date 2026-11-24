package com.zyven.fnfassistant

import android.media.Image
import android.os.SystemClock

/**
 * Visual note detection, plus tap planning while auto-tap is switched on.
 * [process] is only called from the single analysis thread.
 */
object AutoPlayEngine {
    private const val IDLE_OFF_MS = 20_000L

    @Volatile var enabled = false

    private val planner = TapPlanner()
    private var wasTapping = false
    private var lastNoteMs = 0L

    fun process(image: Image) {
        if (!enabled) return
        if (!NoteDetector.analyze(image)) return

        val tapping = AutoTapState.enabled && AutoTapState.serviceConnected
        if (!tapping) {
            if (wasTapping) {
                planner.reset()
                wasTapping = false
            }
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (!wasTapping) {
            wasTapping = true
            lastNoteMs = now
            planner.reset()
        }

        val notes = DetectionState.candidates
        if (notes.isNotEmpty()) {
            lastNoteMs = now
        } else if (now - lastNoteMs > IDLE_OFF_MS) {
            // Safety: no notes for a long time (song over, menu, other app): stop tapping.
            AutoTapState.enabled = false
            AutoTapState.warning = "20 sn nota görünmedi, otomatik basma kapatıldı."
            return
        }

        val plans = planner.update(
            notes,
            now,
            AutoTapState.hitLineY,
            AutoTapState.leadMs.toLong()
        )
        if (plans.isNotEmpty()) AutoTapService.enqueue(plans)
    }
}

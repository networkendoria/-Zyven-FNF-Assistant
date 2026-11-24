package com.zyven.fnfassistant

import android.media.Image
import android.os.SystemClock

// Visual note detection plus optional, user-enabled touch dispatch.
object AutoPlayEngine {
    @Volatile var enabled = true

    fun process(image: Image) {
        if (!enabled) return
        NoteDetector.analyze(image)
        if (!AutoTapState.enabled) return

        val service = AutoTapService.instance ?: return
        val now = SystemClock.elapsedRealtime()
        DetectionState.candidates
            .filter { it.centerY >= 0.78f }
            .forEach { candidate ->
                if (AutoTapState.shouldTap(candidate.lane, now)) {
                    service.tapLane(candidate.lane)
                }
            }
    }
}

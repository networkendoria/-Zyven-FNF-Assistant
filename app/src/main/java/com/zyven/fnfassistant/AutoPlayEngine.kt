package com.zyven.fnfassistant

import android.media.Image

// Visual note detection plus optional, user-enabled touch dispatch.
object AutoPlayEngine {
    @Volatile var enabled = false

    fun process(image: Image) {
        if (!enabled) return
        NoteDetector.analyze(image)
        if (!AutoTapState.enabled) {
            AutoTapState.resetHitZone()
            return
        }
        val service = AutoTapService.instance ?: return
        AutoTapState.newlyReachedHitZone(DetectionState.candidates).forEach { lane ->
            service.tapLane(lane)
        }
    }
}

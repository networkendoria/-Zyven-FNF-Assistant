package com.zyven.fnfassistant

import android.media.Image

// Visual note detection only. This app does not dispatch any touches.
object AutoPlayEngine {
    @Volatile var enabled = false

    fun process(image: Image) {
        if (!enabled) return
        NoteDetector.analyze(image)
    }
}

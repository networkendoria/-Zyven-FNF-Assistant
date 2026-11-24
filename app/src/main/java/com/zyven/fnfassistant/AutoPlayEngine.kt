package com.zyven.fnfassistant

import android.media.Image

// Visual note detection plus optional, user-enabled touch dispatch.
object AutoPlayEngine {
    @Volatile var enabled = false

    fun process(image: Image) {
        if (!enabled) return
        NoteDetector.analyze(image)

    }
}

package com.zyven.fnfassistant

import android.media.Image

// Experimental visual detection only. No touch events are generated.
object AutoPlayEngine {
    @Volatile var enabled = true

    fun process(image: Image) {
        if (enabled) NoteDetector.analyze(image)
    }
}

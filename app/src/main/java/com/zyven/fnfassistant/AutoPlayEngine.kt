package com.zyven.fnfassistant

import android.media.Image

// Compatibility build: screen frames are received but no automatic input is generated.
object AutoPlayEngine {
    @Volatile var enabled = false
    fun process(image: Image) {
        // Intentionally no-op until a trusted distribution and input-control design are ready.
    }
}

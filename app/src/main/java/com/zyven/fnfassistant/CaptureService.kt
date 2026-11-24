package com.zyven.fnfassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AutoPlayEngine.enabled = true
        createChannel()
        startForeground(71, notification())
        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") (intent?.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent)
        if (data == null) {
            DetectionState.active = false
            DetectionState.summary = "Ekran yakalama izni alınamadı"
            stopSelf()
            return START_NOT_STICKY
        }

        // A second start (button pressed twice) replaces the previous capture cleanly.
        releaseCapture()

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(code, data)
        // Android 14+ requires a registered callback before createVirtualDisplay();
        // it is also how we learn that the user stopped sharing from the system UI.
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }
        projectionCallback = callback
        projection?.registerCallback(callback, null)

        val metrics = resources.displayMetrics
        reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)

        // Frames are analyzed on their own thread so the floating menu and the game stay smooth.
        val thread = HandlerThread("zyven-analysis")
        thread.start()
        worker = thread
        reader?.setOnImageAvailableListener({ source ->
            try {
                val frame = source.acquireLatestImage()
                if (frame != null) {
                    try {
                        AutoPlayEngine.process(frame)
                    } finally {
                        frame.close()
                    }
                }
            } catch (error: Exception) {
                DetectionState.summary = "Algılama hatası: ${error.javaClass.simpleName}"
            }
        }, Handler(thread.looper))

        virtualDisplay = projection?.createVirtualDisplay(
            "ZyvenFNFScreen", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null
        )
        DetectionState.summary = "Ekran yakalama başladı • analiz hazırlanıyor"
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("zyven_capture", "Ekran yakalama", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "zyven_capture") else Notification.Builder(this)
        return builder.setContentTitle("Zyven FNF Assistant")
            .setContentText("Dört şeritli nota analizi etkin")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun releaseCapture() {
        reader?.setOnImageAvailableListener(null, null)
        worker?.quitSafely()
        worker = null
        virtualDisplay?.release()
        virtualDisplay = null
        reader?.close()
        reader = null
        projectionCallback?.let { projection?.unregisterCallback(it) }
        projectionCallback = null
        projection?.stop()
        projection = null
    }

    override fun onDestroy() {
        AutoPlayEngine.enabled = false
        AutoTapState.enabled = false
        DetectionState.active = false
        DetectionState.candidates = emptyList()
        DetectionState.summary = "Ekran analizi durduruldu"
        releaseCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_RESULT_DATA = "capture_result_data"
    }
}

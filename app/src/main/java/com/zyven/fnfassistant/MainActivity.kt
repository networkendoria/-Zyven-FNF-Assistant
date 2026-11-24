package com.zyven.fnfassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val captureRequest = 4102
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32); setBackgroundColor(0xFF111018.toInt())
        }
        root.addView(TextView(this).apply {
            text = "ZYVEN FNF ASSISTANT"; textSize = 23f; setTextColor(-1); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Sürüm 0.2.1\nDeneysel dört şeritli nota algılama. Otomatik mod varsayılan olarak kapalıdır. Algılama oyun düzenine göre kalibrasyon gerektirir."
            textSize = 15f; setTextColor(0xFFD2CEDA.toInt()); gravity = Gravity.CENTER; setPadding(0, 22, 0, 22)
        })
        addAction(root, "Yüzen pencere iznini ver") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        addAction(root, "Dokunma erişilebilirliğini aç") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        addAction(root, "Ekran yakalamayı başlat") {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
        }
        addAction(root, "Algılamayı aç / kapat") { AutoPlayEngine.enabled = !AutoPlayEngine.enabled }
        addAction(root, "Kontrol balonunu aç") {
            if (Settings.canDrawOverlays(this)) startService(Intent(this, BubbleService::class.java))
            else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        addAction(root, "Balonu kapat") { stopService(Intent(this, BubbleService::class.java)) }
        addAction(root, "Ekran analizini durdur") { stopService(Intent(this, CaptureService::class.java)) }
        setContentView(root)
    }
    private fun addAction(root: LinearLayout, label: String, action: () -> Unit) {
        root.addView(Button(this).apply { text = label; setOnClickListener { action() } })
    }
    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, CaptureService::class.java).apply {
                putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CaptureService.EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
        }
    }
}

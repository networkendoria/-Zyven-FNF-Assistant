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
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {
    private val captureRequest = 4102
    private val prefs by lazy { getSharedPreferences("zyven_settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFF111018.toInt())
        }
        root.addView(TextView(this).apply {
            text = "ZYVEN FNF ASSISTANT"
            textSize = 23f
            setTextColor(-1)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Sürüm 0.3.0 — oyun profilleri"
            textSize = 15f
            setTextColor(0xFFD2CEDA.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 8)
        })

        root.addView(TextView(this).apply {
            text = "FNF sürümünü seç"
            textSize = 14f
            setTextColor(0xFFE9DDF5.toInt())
            setPadding(0, 12, 0, 6)
        })
        val profiles = FnfProfile.entries
        val selector = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                profiles.map { it.title }
            )
            val savedId = prefs.getString("game_profile", FnfProfile.OFFICIAL_ANDROID.id)
            setSelection(profiles.indexOfFirst { it.id == savedId }.coerceAtLeast(0))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(selector)
        root.addView(TextView(this).apply {
            text = "Seçilen profil bu cihaza kaydedilir. Nota algılama ve otomatik dokunma henüz etkin değil."
            textSize = 12f
            setTextColor(0xFFBDB5C7.toInt())
            setPadding(0, 8, 0, 18)
        })
        addAction(root, "Profili kaydet") {
            val selected = profiles[selector.selectedItemPosition]
            prefs.edit().putString("game_profile", selected.id).apply()
            android.widget.Toast.makeText(this, "Profil: ${selected.title}", android.widget.Toast.LENGTH_SHORT).show()
        }
        addAction(root, "Yüzen pencere iznini ver") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        addAction(root, "Ekran yakalamayı başlat") {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
        }
        addAction(root, "Kontrol balonunu aç") {
            if (Settings.canDrawOverlays(this)) startService(Intent(this, BubbleService::class.java))
            else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        addAction(root, "Balonu kapat") { stopService(Intent(this, BubbleService::class.java)) }
        addAction(root, "Ekran analizini durdur") { stopService(Intent(this, CaptureService::class.java)) }
        setContentView(root)
    }

    private fun addAction(root: LinearLayout, label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        })
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

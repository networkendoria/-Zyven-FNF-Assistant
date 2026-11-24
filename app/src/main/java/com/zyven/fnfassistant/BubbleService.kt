package com.zyven.fnfassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.round

/**
 * Draws the receptor line, the four lane rings and the detected notes over the game.
 * The window is not touchable, so touches go straight through to the game.
 */
internal class GuideView(context: Context) : View(context) {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 255, 214, 64)
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val laneColors = intArrayOf(
        Color.rgb(194, 75, 153), Color.rgb(0, 255, 255), Color.rgb(18, 250, 5), Color.rgb(249, 57, 63)
    )
    private val location = IntArray(2)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val screen = realScreenSize(context)
        if (screen.x <= 0 || screen.y <= 0) return
        getLocationOnScreen(location)
        val centers = NoteDetector.laneCenters
        val hitY = AutoTapState.hitLineY * screen.y - location[1]

        canvas.drawLine(
            centers.first() * screen.x - location[0] - 50f, hitY,
            centers.last() * screen.x - location[0] + 50f, hitY, linePaint
        )
        for (i in centers.indices) {
            ringPaint.color = laneColors[i]
            canvas.drawCircle(centers[i] * screen.x - location[0], hitY, 26f, ringPaint)
        }
        for (note in DetectionState.candidates) {
            val i = note.lane - 1
            if (i !in centers.indices) continue
            dotPaint.color = laneColors[i]
            canvas.drawCircle(
                centers[i] * screen.x - location[0],
                note.centerY * screen.y - location[1], 12f, dotPaint
            )
        }
    }
}

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var guide: GuideView? = null
    private var detectionText: TextView? = null
    private var autoText: TextView? = null
    private var valueText: TextView? = null
    private var toggleButton: Button? = null
    private var guideButton: Button? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("zyven_settings", Context.MODE_PRIVATE) }

    private val ticker = object : Runnable {
        override fun run() {
            guide?.invalidate()
            refreshPanel()
            handler.postDelayed(this, 80L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        AutoTapState.load(prefs)
        if (AutoTapState.showGuide) showGuide()
        showBubble()
        handler.post(ticker)
    }

    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    private fun showGuide() {
        if (guide != null) return
        val view = GuideView(this)
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Android 12+ blocks touches that pass through overlays more opaque than 0.8.
            alpha = 0.75f
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        try {
            wm.addView(view, p)
            guide = view
        } catch (_: Exception) {
        }
    }

    private fun hideGuide() {
        guide?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        guide = null
    }

    private fun showBubble() {
        val b = TextView(this).apply {
            text = "Z"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(133, 53, 205))
                setStroke(2, Color.rgb(210, 160, 255))
            }
        }
        bubble = b
        bubbleParams = params(58, 58).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 240 }
        b.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var dragged = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        sx=e.rawX; sy=e.rawY; ox=bubbleParams.x; oy=bubbleParams.y; dragged=false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=(e.rawX-sx).toInt(); val dy=(e.rawY-sy).toInt()
                        if (kotlin.math.abs(dx)+kotlin.math.abs(dy)>8) dragged=true
                        bubbleParams.x=ox+dx; bubbleParams.y=oy+dy
                        wm.updateViewLayout(b,bubbleParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> { if (!dragged) togglePanel(); return true }
                }
                return false
            }
        })
        wm.addView(b, bubbleParams)
    }

    private fun smallButton(label: String, onClick: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 11f
        isAllCaps = false
        minHeight = 0
        minimumHeight = 0
        setPadding(8, 6, 8, 6)
        setOnClickListener { onClick() }
    }

    private fun row(vararg buttons: Button): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        for (button in buttons) {
            addView(button, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun togglePanel() {
        panel?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
            panel = null
            detectionText = null
            autoText = null
            valueText = null
            toggleButton = null
            guideButton = null
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            background = GradientDrawable().apply {
                setColor(Color.rgb(24, 21, 32)); cornerRadius = 24f
                setStroke(2, Color.rgb(145, 75, 210))
            }
        }
        box.addView(TextView(this).apply {
            text = "ZYVEN FNF ASSISTANT"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(4, 4, 4, 12)
        })
        detectionText = TextView(this).apply {
            text = DetectionState.summary
            textSize = 12f
            setTextColor(0xFFD0C8DA.toInt())
            setPadding(4, 4, 4, 10)
        }
        box.addView(detectionText)
        autoText = TextView(this).apply {
            text = autoStatus()
            textSize = 11f
            setTextColor(0xFFB7AFC1.toInt())
            setPadding(4, 2, 4, 8)
        }
        box.addView(autoText)

        toggleButton = smallButton("") { onToggleAuto() }
        box.addView(toggleButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        valueText = TextView(this).apply {
            textSize = 11f
            setTextColor(0xFFD0C8DA.toInt())
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 4)
        }
        box.addView(valueText)
        box.addView(row(
            smallButton("Çizgi ▲") { adjustLine(-0.005f) },
            smallButton("Çizgi ▼") { adjustLine(0.005f) }
        ))
        box.addView(row(
            smallButton("Erken −") { adjustLead(-10) },
            smallButton("Erken +") { adjustLead(10) }
        ))
        guideButton = smallButton("") { toggleGuide() }
        box.addView(row(
            guideButton!!,
            smallButton("Test dokunuşu") { onTestTaps() }
        ))
        box.addView(smallButton("Menüyü kapat") { togglePanel() }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        panel = box
        refreshPanel()

        val maxY = (realScreenSize(this).y - 480).coerceAtLeast(0)
        val p = params(380, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (bubbleParams.x - 205).coerceAtLeast(0)
            y = (bubbleParams.y + 65).coerceAtLeast(0).coerceAtMost(maxY)
        }
        wm.addView(box, p)
    }

    private fun autoStatus(): String {
        val base = when {
            !AutoTapState.serviceConnected ->
                "Erişilebilirlik servisi kapalı. Açmak için aşağıdaki düğmeye bas."
            AutoTapState.enabled -> "Otomatik basma AÇIK • ${AutoTapState.tapCount} dokunuş"
            else -> "Otomatik basma KAPALI"
        }
        val warning = AutoTapState.warning
        return if (warning.isEmpty()) base else "$base\n$warning"
    }

    private fun refreshPanel() {
        if (panel == null) return
        setIfChanged(detectionText, DetectionState.summary)
        setIfChanged(autoText, autoStatus())
        setIfChanged(
            valueText,
            "Çizgi: ${"%.3f".format(AutoTapState.hitLineY)}  •  Erken: ${AutoTapState.leadMs} ms"
        )
        setIfChanged(
            toggleButton,
            when {
                !AutoTapState.serviceConnected -> "Erişilebilirliği aç"
                AutoTapState.enabled -> "Otomatik basma: AÇIK"
                else -> "Otomatik basma: KAPALI"
            }
        )
        setIfChanged(guideButton, if (AutoTapState.showGuide) "Rehber: AÇIK" else "Rehber: KAPALI")
    }

    private fun setIfChanged(view: TextView?, value: String) {
        if (view != null && view.text.toString() != value) view.text = value
    }

    private fun onToggleAuto() {
        if (!AutoTapState.serviceConnected) {
            openAccessibilitySettings()
            return
        }
        if (AutoTapState.enabled) {
            AutoTapState.enabled = false
            return
        }
        if (!DetectionState.active) {
            AutoTapState.warning = "Önce ana ekrandan ekran yakalamayı başlat."
            return
        }
        AutoTapState.warning = ""
        AutoTapState.tapCount = 0
        AutoTapState.enabled = true
    }

    private fun onTestTaps() {
        if (!AutoTapState.serviceConnected) {
            AutoTapState.warning = "Test için önce erişilebilirlik servisini aç."
            return
        }
        AutoTapState.warning = ""
        AutoTapService.testTaps()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            AutoTapState.warning = "Erişilebilirlik ayarları açılamadı. Ayarlar'dan elle aç."
        }
    }

    private fun adjustLine(delta: Float) {
        val next = round((AutoTapState.hitLineY + delta) * 1000f) / 1000f
        AutoTapState.hitLineY = next.coerceIn(AutoTapState.HIT_LINE_MIN, AutoTapState.HIT_LINE_MAX)
        AutoTapState.save(prefs)
    }

    private fun adjustLead(delta: Int) {
        AutoTapState.leadMs = (AutoTapState.leadMs + delta).coerceIn(AutoTapState.LEAD_MIN, AutoTapState.LEAD_MAX)
        AutoTapState.save(prefs)
    }

    private fun toggleGuide() {
        AutoTapState.showGuide = !AutoTapState.showGuide
        if (AutoTapState.showGuide) showGuide() else hideGuide()
        AutoTapState.save(prefs)
    }

    override fun onDestroy() {
        // Closing the bubble always stops auto-tap.
        AutoTapState.enabled = false
        handler.removeCallbacks(ticker)
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        hideGuide()
        panel = null; bubble = null
        detectionText = null; autoText = null; valueText = null
        toggleButton = null; guideButton = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

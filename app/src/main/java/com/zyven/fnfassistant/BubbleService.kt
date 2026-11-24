package com.zyven.fnfassistant

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var detectionText: TextView? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private val refreshStatus = object : Runnable {
        override fun run() {
            detectionText?.text = DetectionState.summary
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

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

    private fun togglePanel() {
        panel?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
            panel=null
            detectionText=null
            handler.removeCallbacks(refreshStatus)
            return
        }
        val box = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(18,16,18,16)
            background=GradientDrawable().apply {
                setColor(Color.rgb(24,21,32)); cornerRadius=24f
                setStroke(2,Color.rgb(145,75,210))
            }
        }
        box.addView(TextView(this).apply {
            text="ZYVEN FNF ASSISTANT"
            textSize=16f
            setTextColor(Color.WHITE)
            gravity=Gravity.CENTER
            setPadding(4,4,4,12)
        })
        detectionText = TextView(this).apply {
            text=DetectionState.summary
            textSize=12f
            setTextColor(0xFFD0C8DA.toInt())
            setPadding(4,4,4,12)
        }
        box.addView(detectionText)
        box.addView(TextView(this).apply {
            text="Deneysel algılama: renkli nota adayları. Otomatik dokunma kapalı."
            textSize=11f
            setTextColor(0xFFB7AFC1.toInt())
            setPadding(4,4,4,10)
        })
        box.addView(Button(this).apply {
            text="Menüyü kapat"
            setOnClickListener { togglePanel() }
        })
        panel=box
        val p=params(290,WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity=Gravity.TOP or Gravity.START
            x=(bubbleParams.x-205).coerceAtLeast(0)
            y=(bubbleParams.y+65).coerceAtLeast(0)
        }
        wm.addView(box,p)
        handler.post(refreshStatus)
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshStatus)
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel=null; bubble=null; detectionText=null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.zyven.fnfassistant

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    override fun onCreate() { super.onCreate(); wm = getSystemService(WINDOW_SERVICE) as WindowManager; showBubble() }
    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )
    private fun showBubble() {
        val b = TextView(this).apply {
            text = "Z"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(Color.rgb(133, 53, 205))
                setStroke(2, Color.rgb(210, 160, 255))
            }
        }
        bubble = b
        bubbleParams = params(58, 58).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 240 }
        b.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var dragged = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx=e.rawX; sy=e.rawY; ox=bubbleParams.x; oy=bubbleParams.y; dragged=false; return true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=(e.rawX-sx).toInt(); val dy=(e.rawY-sy).toInt()
                        if (kotlin.math.abs(dx)+kotlin.math.abs(dy)>8) dragged=true
                        bubbleParams.x=ox+dx; bubbleParams.y=oy+dy; wm.updateViewLayout(b,bubbleParams); return true
                    }
                    MotionEvent.ACTION_UP -> { if (!dragged) togglePanel(); return true }
                }
                return false
            }
        })
        wm.addView(b, bubbleParams)
    }
    private fun togglePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {}; panel=null; return }
        val box = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(18,16,18,16)
            background=GradientDrawable().apply {
                setColor(Color.rgb(24,21,32)); cornerRadius=24f; setStroke(2,Color.rgb(145,75,210))
            }
        }
        box.addView(TextView(this).apply {
            text="ZYVEN ASSISTANT"; textSize=16f; setTextColor(Color.WHITE)
            gravity=Gravity.CENTER; setPadding(4,4,4,12)
        })
        box.addView(TextView(this).apply {
            text="İlk sürüm: kontrol ve yakalama testi. Otomatik nota tanıma henüz yok."
            textSize=12f; setTextColor(0xFFD0C8DA.toInt())
        })
        listOf("SOL","AŞAĞI","YUKARI","SAĞ").forEachIndexed { i, label ->
            box.addView(Button(this).apply {
                text="$label • test dokunuşu"
                setOnClickListener {
                    val x=listOf(.20f,.40f,.60f,.80f)[i]
                    val service=TapAccessibilityService.instance
                    if (service==null) Toast.makeText(this@BubbleService,"Önce Erişilebilirlik hizmetini etkinleştir",Toast.LENGTH_SHORT).show()
                    else { service.tapNormalized(x,.88f); Toast.makeText(this@BubbleService,"$label test dokunuşu gönderildi",Toast.LENGTH_SHORT).show() }
                }
            })
        }
        box.addView(Button(this).apply { text="Menüyü kapat"; setOnClickListener { togglePanel() } })
        panel=box
        val p=params(270,WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity=Gravity.TOP or Gravity.START; x=(bubbleParams.x-190).coerceAtLeast(0); y=(bubbleParams.y+65).coerceAtLeast(0)
        }
        wm.addView(box,p)
    }
    override fun onDestroy() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel=null; bubble=null; super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

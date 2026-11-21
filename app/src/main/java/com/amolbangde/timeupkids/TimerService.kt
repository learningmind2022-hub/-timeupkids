package com.amolbangde.timeupkids

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class TimerService : Service() {

    companion object {
        const val EXTRA_MIN = "minutes"
        const val ACTION_STOP = "stop"
        private const val CHANNEL_ID = "timeup_kids"
        private const val WARNING_MS = 3 * 60 * 1000L   // last 3 minutes: eyes countdown
        private const val URGENT_MS = 30 * 1000L        // last 30 seconds: bigger, faster eyes
        private const val BRAIN_SECONDS = 5L            // crying brain screen duration before lock
    }

    private var timer: CountDownTimer? = null
    private var brainTimer: CountDownTimer? = null

    private var overlay: LinearLayout? = null
    private var eyes: EyesView? = null
    private var timeText: TextView? = null
    private var brainScreen: LinearLayout? = null

    private val wm by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            cleanup(); stopSelf(); return START_NOT_STICKY
        }
        val minutes = intent?.getIntExtra(EXTRA_MIN, 10) ?: 10

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "TimeUp Kids", NotificationManager.IMPORTANCE_LOW))
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("TimeUp Kids is running")
            .setContentText("Phone locks in $minutes minutes")
            .setSubText("Developed by ${MainActivity.DEVELOPER}")
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, notif,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        cleanup()
        timer = object : CountDownTimer(minutes * 60_000L, 1000L) {
            override fun onTick(ms: Long) {
                if (ms <= WARNING_MS) showCountdown(ms)
            }
            override fun onFinish() {
                // Pehle crying brain screen, phir phone lock
                showCryingBrain {
                    removeBrain()
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    dpm.lockNow()
                    stopSelf()
                }
            }
        }.start()
        return START_NOT_STICKY
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ---------- Last 3 minutes: eyes + countdown ----------
    private fun showCountdown(ms: Long) {
        val s = ((ms + 999) / 1000).toInt()
        if (overlay == null) {
            val eyesView = EyesView(this)
            val tv = TextView(this).apply {
                textSize = 26f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(8), 0, 0, 0)
            }
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(8), dp(16), dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E6D32F2F"))
                    cornerRadius = dp(20).toFloat()
                }
                addView(eyesView)
                addView(tv)
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.END; y = dp(60); x = dp(8) }
            wm.addView(box, params)
            overlay = box; eyes = eyesView; timeText = tv
        }
        eyes?.urgent = ms <= URGENT_MS
        timeText?.text = "%02d:%02d".format(s / 60, s % 60)
    }

    private fun removeOverlay() {
        overlay?.let { runCatching { wm.removeView(it) } }
        overlay = null; eyes = null; timeText = null
    }

    // ---------- Timer end: crying brain full screen ----------
    private fun showCryingBrain(onDone: () -> Unit) {
        removeOverlay()

        val brain = CryingBrainView(this)
        val title = TextView(this).apply {
            text = "Time is up!"
            textSize = 28f
            setTextColor(Color.parseColor("#FF8A8A"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "My brain is tired and sad...\nPlease take a break."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        val count = TextView(this).apply {
            text = "Phone locks in $BRAIN_SECONDS"
            textSize = 14f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#6B0F1F"), Color.parseColor("#1A0308"))
            )
            addView(brain, LinearLayout.LayoutParams(dp(260), dp(247)))
            addView(title)
            addView(sub)
            addView(count)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        try {
            wm.addView(box, params)
            brainScreen = box
        } catch (e: Exception) {
            // Overlay permission hat gayi ho to seedha lock kar do
            onDone()
            return
        }

        brainTimer?.cancel()
        brainTimer = object : CountDownTimer(BRAIN_SECONDS * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                count.text = "Phone locks in ${(ms + 999) / 1000}"
            }
            override fun onFinish() { onDone() }
        }.start()
    }

    private fun removeBrain() {
        brainTimer?.cancel(); brainTimer = null
        brainScreen?.let { runCatching { wm.removeView(it) } }
        brainScreen = null
    }

    private fun cleanup() {
        timer?.cancel(); timer = null
        removeOverlay()
        removeBrain()
    }

    override fun onDestroy() { cleanup(); super.onDestroy() }
}

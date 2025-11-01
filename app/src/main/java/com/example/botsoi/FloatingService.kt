package com.example.botsoi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import android.widget.ImageView

class FloatingService : Service() {

    companion object {
        const val CHANNEL_ID = "floating_service_channel"
        const val NOTIF_ID = 101
        const val EXTRA_GAME_NAME = "extra_game_name"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: "Game"

        startForeground(NOTIF_ID, createNotification("Đang chạy: $gameName"))

        if (overlayView != null) {
            overlayView?.findViewById<TextView>(R.id.overlay_table_name)?.text = gameName
            return START_STICKY
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 200
            y = 300
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_view, null)

        val gifView = overlayView!!.findViewById<ImageView>(R.id.overlay_gif)
        val titleView = overlayView!!.findViewById<TextView>(R.id.overlay_table_name)
        titleView.text = gameName

        val gifList = listOf(
            R.drawable.robot,
            R.drawable.robot1,
            R.drawable.robot2,
            R.drawable.robot3,
        )

        var currentGif = 0
        fun loadGif(i: Int) {
            Glide.with(this).asGif().load(gifList[i]).into(gifView)
        }
        loadGif(currentGif)

        var lastClickTime = 0L
        val doubleClickTime = 300L
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        gifView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams!!.x
                    startY = layoutParams!!.y
                    touchX = e.rawX
                    touchY = e.rawY
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < doubleClickTime) {
                        currentGif = (currentGif + 1) % gifList.size
                        loadGif(currentGif)
                    }
                    lastClickTime = now
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams!!.x = startX + (e.rawX - touchX).toInt()
                    layoutParams!!.y = startY + (e.rawY - touchY).toInt()
                    windowManager!!.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }

        overlayView!!.findViewById<View>(R.id.overlay_close).setOnClickListener {
            stopSelf()
        }

        windowManager!!.addView(overlayView, layoutParams)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BotSoi Floating")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "Floating Service", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }
}

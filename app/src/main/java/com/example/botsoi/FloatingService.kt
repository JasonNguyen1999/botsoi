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
        const val EXTRA_TABLE_ID = "extra_table_id"
        const val EXTRA_TABLE_NAME = "extra_table_name"
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
        val tableId = intent?.getIntExtra(EXTRA_TABLE_ID, 0) ?: 0
        val tableName = intent?.getStringExtra(EXTRA_TABLE_NAME) ?: "Bàn"

        startForeground(NOTIF_ID, createNotification("Đang thu nhỏ: $tableName"))

        // Nếu overlay đã hiển thị thì cập nhật text
        if (overlayView != null) {
            (overlayView?.findViewById<TextView>(R.id.overlay_table_name))?.text = tableName
            return START_STICKY
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // layout params: use TYPE_APPLICATION_OVERLAY for Android O+
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // inflate custom overlay view
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_view, null)
        val gifView = overlayView!!.findViewById<ImageView>(R.id.overlay_gif)

        // Danh sách ảnh GIF
                val gifList = listOf(
                    R.drawable.robot,
                    R.drawable.robot1,
                    R.drawable.robot2,
                )

        var currentGifIndex = 0

        // Hàm load ảnh GIF
        fun loadGif(index: Int) {
            Glide.with(this)
                .asGif()
                .load(gifList[index])
                .into(gifView)
        }
        loadGif(currentGifIndex)

// --- Kéo và double-click trong cùng 1 listener ---
        var lastClickTime = 0L
        val doubleClickInterval = 300L // ms

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        gifView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    touchX = event.rawX
                    touchY = event.rawY

                    // kiểm tra double click
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < doubleClickInterval) {
                        currentGifIndex = (currentGifIndex + 1) % gifList.size
                        loadGif(currentGifIndex)
                    }
                    lastClickTime = currentTime
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    layoutParams?.x = initialX + dx
                    layoutParams?.y = initialY + dy
                    windowManager?.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }
//        val gifView = overlayView!!.findViewById<ImageView>(R.id.overlay_gif)
        Glide.with(this).asGif().load(R.drawable.robot).into(gifView)

        // set table name text
        val titleView = overlayView!!.findViewById<TextView>(R.id.overlay_table_name)
        titleView.text = tableName

        // close button
        overlayView!!.findViewById<View>(R.id.overlay_close).setOnClickListener {
            stopSelf()
        }

        // click on overlay -> open MainActivity (restore)
        overlayView!!.setOnClickListener {
            val openIntent = Intent(this, MainActivity::class.java)
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(openIntent)
            // optionally stop service (hide overlay)
            stopSelf()
        }

        // handle drag
        overlayView!!.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var touchX = 0f
            var touchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        layoutParams?.x = initialX + dx
                        layoutParams?.y = initialY + dy
                        windowManager?.updateViewLayout(overlayView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })

        // add view
        windowManager?.addView(overlayView, layoutParams)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Service")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "Floating Service", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
    }
}

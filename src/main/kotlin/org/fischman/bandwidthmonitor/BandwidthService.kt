package org.fischman.bandwidthmonitor

import android.app.Notification
import android.app.Notification.MetricStyle
import android.app.Notification.Metric
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class BandwidthService : Service(), Runnable {
    private lateinit var handler: Handler
    private lateinit var nm: NotificationManager
    private var prevRx = 0L
    private var prevTx = 0L

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHAN, "Bandwidth", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
        )
        handler = Handler(Looper.getMainLooper())
        prevRx = TrafficStats.getTotalRxBytes()
        prevTx = TrafficStats.getTotalTxBytes()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotif(0L, 0L))
        handler.removeCallbacks(this)
        handler.post(this)
        return START_STICKY
    }

    override fun run() {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val rxSpeed = rx - prevRx
        val txSpeed = tx - prevTx
        prevRx = rx
        prevTx = tx
        nm.notify(NOTIF_ID, buildNotif(rxSpeed, txSpeed))
        handler.postDelayed(this, INTERVAL_MS)
    }

    private fun buildNotif(
        rxSpeed: Long,
        txSpeed: Long,
    ): Notification {
        val b = Notification.Builder(this, CHAN)
        b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)

        if (Build.VERSION.SDK_INT >= 37) {
            val (rf, ru) = fmtFloat(rxSpeed)
            val (tf, tu) = fmtFloat(txSpeed)
            b
                .setRequestPromotedOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setSubText("tap to stop monitoring")
                .setStyle(MetricStyle()
                    .addMetric(Metric(Metric.FixedFloat(rf, ru, 0, 1), "Download"))
                    .addMetric(Metric(Metric.FixedFloat(tf, tu, 0, 1), "Upload"))
                )
        } else {
            b.setSmallIcon(buildIcon(rxSpeed, txSpeed))
        }
        return b
            .setContentTitle(applicationInfo.loadLabel(packageManager).toString())
            .setContentText("Tap to stop monitoring")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, BandwidthService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
    }

    private fun buildIcon(
        rxSpeed: Long,
        txSpeed: Long,
    ): Icon {
        val size = 96 // Android constrains notification icons to square :/
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint =
            Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            }
        val txText = "\u2191" + fmtShort(txSpeed)
        val rxText = "\u2193" + fmtShort(rxSpeed)
        drawCenteredText(canvas, txText, size / 2f, size.toFloat() / 4, paint, size.toFloat())
        drawCenteredText(canvas, rxText, size / 2f, size.toFloat() * 3 / 4, paint, size.toFloat())

        return Icon.createWithBitmap(bitmap)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        paint: Paint,
        maxWidth: Float,
    ) {
        paint.textSize = maxWidth / 2
        val computedWidth = paint.measureText(text)
        if (maxWidth < computedWidth) {
            paint.textSize *= maxWidth / computedWidth
        }
        val fontMetrics = paint.fontMetrics
        val textOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(text, cx, cy - textOffset, paint)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(this)
        super.onDestroy()
    }

    companion object {
        private const val CHAN = "bw"
        private const val NOTIF_ID = 1
        private const val INTERVAL_MS = 1000L
        private const val ACTION_STOP = "org.fischman.bandwidthmonitor.STOP"

        fun fmtShort(bytes: Long): String =
            when {
                bytes < 1024L -> "${bytes}B"
                bytes < 1024L * 10L -> "%.1fK".format(bytes / 1024.0)
                bytes < 1024L * 1024L -> "${bytes / 1024L}K"
                bytes < 1024L * 1024L * 10L -> "%.1fM".format(bytes / (1024.0 * 1024.0))
                else -> "${bytes / (1024L * 1024L)}M"
            }

        fun fmtFloat(bytes: Long): Pair<Float, String> {
            val b = bytes.toFloat()
            return when {
                b < 100 -> b to "B"
                b < 100_000 -> b / 1_024f to "KiB"
                b < 100_000_000 -> b / 1_048_576f to "MiB"
                else -> b / 1_073_741_824f to "GiB"
            }
        }

    }
}

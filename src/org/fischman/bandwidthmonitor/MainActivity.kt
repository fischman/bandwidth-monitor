package org.fischman.bandwidthmonitor

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity(), View.OnClickListener {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
            }

        status =
            TextView(this).apply {
                textSize = 18f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
        root.addView(status)

        setContentView(root)
        tryStart()
    }

    override fun onClick(v: View) = tryStart()

    private fun needPerm(): Boolean =
        (
            (Build.VERSION.SDK_INT >= 33) &&
                (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        )

    private fun tryStart() {
        if (needPerm()) {
            status.text = "Notification permission needed."
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        startMonitoring()
    }

    override fun onRequestPermissionsResult(
        req: Int,
        perms: Array<String>,
        results: IntArray,
    ) {
        if (req == 1 && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startMonitoring()
        } else {
            status.text = "Permission denied. Grant in system settings."
        }
    }

    private fun startMonitoring() {
        status.text = "Monitoring bandwidth\u2026\nCheck your notification bar."
        val intent = Intent(this, BandwidthService::class.java)
        startForegroundService(intent)
        finish()
    }
}

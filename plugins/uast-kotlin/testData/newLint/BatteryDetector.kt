package test.pkg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class BatteryTest : Activity() {
    fun testNoNo() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:my.pkg")
        startActivity(intent)
    }
}
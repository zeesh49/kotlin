package test.pkg

import android.content.ClipboardManager
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.service.wallpaper.WallpaperService

class SystemServiceTest : Activity() {

    fun test1() {
        val displayServiceOk = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayServiceWrong = getSystemService(Context.DEVICE_POLICY_SERVICE) as DisplayManager
        val wallPaperOk = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperService
        val wallPaperWrong = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
    }

    fun test2(context: Context) {
        val displayServiceOk = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayServiceWrong = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DisplayManager
    }

    fun clipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipboard1 = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipboard2 = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
    }
}

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView

class SetJavaScriptEnabled : Activity() {
    public override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val webView = findViewById(R.id.webView) as WebView
        webView.settings.setJavaScriptEnabled(true) // bad
        webView.settings.javaScriptEnabled = true // bad
        webView.settings.setJavaScriptEnabled(false) // good
        webView.settings.javaScriptEnabled = false // good
        webView.loadUrl("file:///android_asset/www/index.html")
    }


    // Test Suppress
    // Constructor: See issue 35588
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    fun HelloWebApp() {
        val webView = findViewById(R.id.webView) as WebView
        webView.settings.setJavaScriptEnabled(true) // bad
        webView.settings.javaScriptEnabled = true // bad
        webView.settings.setJavaScriptEnabled(false) // good
        webView.settings.javaScriptEnabled = false // good
        webView.loadUrl("file:///android_asset/www/index.html")
    }

    object R {
        object layout {
            val main = 0x7f0a0000
        }

        object id {
            val webView = 0x7f0a0001
        }
    }
}
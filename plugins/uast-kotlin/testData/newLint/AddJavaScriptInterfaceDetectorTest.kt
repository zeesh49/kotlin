package test.pkg

import android.webkit.WebView
import android.content.Context


class AddJavascriptInterfaceTest {
    private class WebViewChild internal constructor(context: Context) : WebView(context)

    private class CallAddJavascriptInterfaceOnWebView {
        fun addJavascriptInterfaceToWebView(webView: WebView, `object`: Any, string: String) {
            webView.addJavascriptInterface(`object`, string)
        }
    }

    private class CallAddJavascriptInterfaceOnWebViewChild {
        fun addJavascriptInterfaceToWebViewChild(
                webView: WebViewChild, `object`: Any, string: String) {
            webView.addJavascriptInterface(`object`, string)
        }
    }

    private class NonWebView {
        fun addJavascriptInterface(`object`: Any, string: String) {
        }
    }

    private class CallAddJavascriptInterfaceOnNonWebView {
        fun addJavascriptInterfaceToNonWebView(
                webView: NonWebView, `object`: Any, string: String) {
            webView.addJavascriptInterface(`object`, string)
        }
    }
}
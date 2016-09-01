import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Message
import android.os.Looper

class HandlerTest : Handler() { // OK
    class StaticInner : Handler() { // OK
        override fun dispatchMessage(msg: Message) {
            super.dispatchMessage(msg)
        }
    }

    inner class Inner : Handler() { // ERROR
        override fun dispatchMessage(msg: Message) {
            super.dispatchMessage(msg)
        }
    }


    internal fun method() {
        val anonymous = object : Handler() { // ERROR
            override fun dispatchMessage(msg: Message) {
                super.dispatchMessage(msg)
            }
        }

        val looper: Looper? = null
        val anonymous2 = object : Handler(looper) { // OK
            override fun dispatchMessage(msg: Message) {
                super.dispatchMessage(msg)
            }
        }
    }

    inner class WithArbitraryLooper(unused: String, looper: Looper)// OK
    : Handler(looper, null) {

        override fun dispatchMessage(msg: Message) {
            super.dispatchMessage(msg)
        }
    }
}


class CheckActivity : Activity() {

    @SuppressWarnings("unused")
    @SuppressLint("HandlerLeak")
    internal var handler: Handler = object : Handler() {

        override fun handleMessage(msg: Message) {

        }
    }

}
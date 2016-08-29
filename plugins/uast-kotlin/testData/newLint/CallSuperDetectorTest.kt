package com.example.yan.myapplication

import android.view.View

class DetachedFromWindow {
    private class Test1 : ViewWithDefaultConstructor() {
        override  fun onDetachedFromWindow() {
            // Error
        }
    }

    private class Test2 : ViewWithDefaultConstructor() {
        fun onDetachedFromWindow(foo: Int) {
            // OK: not overriding the right method
        }
    }

    private class Test3 : ViewWithDefaultConstructor() {
        override  fun onDetachedFromWindow() {
            // OK: Calling super
            super.onDetachedFromWindow()
        }
    }

    private class Test4 : ViewWithDefaultConstructor() {
        override fun onDetachedFromWindow() {
            // Error: missing detach call
            var x = 1
            x++
            println(x)
        }
    }

    private class Test5 : Any() {
        protected fun onDetachedFromWindow() {
            // OK - not in a view
            // Regression test for http://b.android.com/73571
        }
    }

    open class ViewWithDefaultConstructor : View(null)
}

package test.pkg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Rect
import android.graphics.Shader.TileMode
import android.util.AttributeSet
import android.util.SparseArray
import android.widget.Button
import java.util.HashMap

/** Some test data for the JavaPerformanceDetector  */
@SuppressWarnings("unused")
open class JavaPerformanceTest : Button {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    private var cachedRect: Rect? = null

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)

        // Various allocations:
        java.lang.String("foo")
        val s = java.lang.String("bar")

        // This one should not be reported:
        @SuppressLint("DrawAllocation")
        val i = java.lang.String("f")

        // Cached object initialized lazily: should not complain about these
        if (cachedRect == null) {
            cachedRect = Rect(0, 0, 100, 100)
        }
        if (cachedRect == null || cachedRect!!.width() != 50) {
            cachedRect = Rect(0, 0, 50, 100)
        }

        val b = java.lang.Boolean.valueOf(true)!! // auto-boxing
        dummy(1, 2)

        // Non-allocations
        super.animate()
        dummy2(1, 2)
        val x = 4 + '5'.toInt()

        // This will involve allocations, but we don't track
        // inter-procedural stuff here
        someOtherMethod()
    }

    internal fun dummy(foo: Int?, bar: Int) {
        dummy2(foo!!, bar)
    }

    internal fun dummy2(foo: Int, bar: Int) {
    }

    internal fun someOtherMethod() {
        // Allocations are okay here
        java.lang.String("foo")
        val s = java.lang.String("bar")
        val b = java.lang.Boolean.valueOf(true) // auto-boxing

        // Sparse array candidates
        val myMap = HashMap<Int, String>()
        // Should use SparseBooleanArray
        val myBoolMap = HashMap<Int, Boolean>()
        // Should use SparseIntArray
        val myIntMap = java.util.HashMap<Int, Int>()

        // This one should not be reported:
        @SuppressLint("UseSparseArrays")
        val myOtherMap = HashMap<Int, Any>()
    }

    protected fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int,
                            x: Boolean) { // wrong signature
        java.lang.String("not an error")
    }

    protected fun onMeasure(widthMeasureSpec: Int) { // wrong signature
        java.lang.String("not an error")
    }

    protected fun onLayout(changed: Boolean, left: Int, top: Int, right: Int,
                           bottom: Int, wrong: Int) { // wrong signature
        java.lang.String("not an error")
    }

    protected fun onLayout(changed: Boolean, left: Int, top: Int, right: Int) {
        // wrong signature
        java.lang.String("not an error")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int,
                          bottom: Int) {
        java.lang.String("flag me")
    }

    @SuppressWarnings("null") // not real code
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        java.lang.String("flag me")

        // Forbidden factory methods:
        Bitmap.createBitmap(100, 100, null)
        android.graphics.Bitmap.createScaledBitmap(null, 100, 100, false)
        BitmapFactory.decodeFile(null)
        val canvas: Canvas? = null
        canvas!!.clipBounds // allocates on your behalf
        canvas.getClipBounds(null) // NOT an error

        val layoutWidth = width
        val layoutHeight = height
        if (mAllowCrop && (mOverlay == null || mOverlay!!.width != layoutWidth ||
                           mOverlay!!.height != layoutHeight)) {
            mOverlay = Bitmap.createBitmap(layoutWidth, layoutHeight, Bitmap.Config.ARGB_8888)
            mOverlayCanvas = Canvas(mOverlay)
        }

        if (widthMeasureSpec == 42) {
            throw IllegalStateException("Test") // NOT an allocation
        }

        // More lazy init tests
        var initialized = false
        if (!initialized) {
            java.lang.String("foo")
            initialized = true
        }

        // NOT lazy initialization
        if (!initialized || mOverlay == null) {
            java.lang.String("foo")
        }
    }

    internal fun factories() {
        val i1 = 42
        val l1 = 42L
        val b1 = true
        val c1 = 'c'
        val f1 = 1.0f
        val d1 = 1.0

        // The following should not generate errors:
        val i3 = Integer.valueOf(42)
    }

    private val mAllowCrop: Boolean = false
    private var mOverlayCanvas: Canvas? = null
    private var mOverlay: Bitmap? = null

    private abstract inner class JavaPerformanceTest1 : JavaPerformanceTest() {
        override fun layout(l: Int, t: Int, r: Int, b: Int) {
            // Using "this." to reference fields
            if (this.shader == null)
                this.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, GRADIENT_COLORS, null,
                                             TileMode.REPEAT)
        }
    }

    private abstract inner class JavaPerformanceTest2 : JavaPerformanceTest() {
        override fun layout(l: Int, t: Int, r: Int, b: Int) {
            val width = width
            val height = height

            if (shader == null || lastWidth != width || lastHeight != height) {
                lastWidth = width
                lastHeight = height

                shader = LinearGradient(0f, 0f, width.toFloat(), 0f, GRADIENT_COLORS, null, TileMode.REPEAT)
            }
        }
    }

    private abstract inner class JavaPerformanceTest3 : JavaPerformanceTest() {
        override fun layout(l: Int, t: Int, r: Int, b: Int) {
            if (shader == null || lastWidth != width || lastHeight != height) {
            }
        }
    }

    fun inefficientSparseArray() {
        SparseArray<Int>() // Use SparseIntArray instead
        SparseArray<Long>()    // Use SparseLongArray instead
        SparseArray<Boolean>() // Use SparseBooleanArray instead
        SparseArray<Any>()  // OK
    }

    fun longSparseArray() { // but only minSdkVersion >= 17 or if has v4 support lib
        val myStringMap = HashMap<Long, String>()
    }

    fun byteSparseArray() { // bytes easily apply to ints
        val myByteMap = HashMap<Byte, String>()
    }

    protected var shader: LinearGradient? = null
    protected var lastWidth: Int = 0
    protected var lastHeight: Int = 0
    protected var GRADIENT_COLORS: IntArray? = null

    private class foo {
        private class bar {
            private class Integer(`val`: Int)
        }
    }

    constructor() : super(null) {
    }
}
package android.util

object FloatMath {
    fun sin(x: Float): Float = 0f
    fun cos(x: Float): Float = 0f
    fun ceil(x: Float): Float = 0f
    fun floor(x: Float): Float = 0f
    fun sqrt(x: Int): Float = 0f
}

//Test data for the MathDetector
class MathTest {
    var floatResult: Float = 0.toFloat()

    fun floatToFloatTest(x: Float, y: Double, z: Int) {
        floatResult = FloatMath.cos(x)
        floatResult = FloatMath.sin(y.toFloat())
        floatResult = android.util.FloatMath.ceil(y.toFloat())
        System.out.println(FloatMath.floor(x))
        System.out.println(FloatMath.sqrt(z))

        // No warnings for plain math
        floatResult = Math.cos(x.toDouble()).toFloat()
        floatResult = java.lang.Math.sin(x.toDouble()).toFloat()
    }
}

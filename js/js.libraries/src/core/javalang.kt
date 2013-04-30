package java.lang

import java.io.IOException
import js.library

library
open public class Exception(message: String? = null) : Throwable() {}

library
open public class RuntimeException(message: String? = null) : Exception(message) {}

library("splitString")
public fun String.split(regex : String) : Array<String> = js.noImpl

library
public class IllegalArgumentException(message: String? = null) : Exception() {}

library
public class IllegalStateException(message: String? = null) : Exception() {}

library
public class IndexOutOfBoundsException(message: String? = null) : Exception() {}

library
public class UnsupportedOperationException(message: String? = null) : Exception() {}

library
public class NumberFormatException(message: String? = null) : Exception() {}

library
public trait Runnable {
    public open fun run() : Unit;
}

library
public trait Comparable<T> {
    public fun compareTo(that: T): Int
}

library
public trait Appendable {
    public open fun append(csq: CharSequence?): Appendable?
    public open fun append(csq: CharSequence?, start: Int, end: Int): Appendable?
    public open fun append(c: Char): Appendable?
}

//todo maybe need to replace to class object
native("Number")
public object Float {
    public val MAX_VALUE: Float
    public val MIN_VALUE: Float

    public val POSITIVE_INFINITY: Float
    public val NEGATIVE_INFINITY: Float

    public val NaN: Float

    public fun isNaN(value: Float): Boolean
}

native("Number")
public object Double {
    public val MAX_VALUE: Double
    public val MIN_VALUE: Double

    public val POSITIVE_INFINITY: Double
    public val NEGATIVE_INFINITY: Double

    public val NaN: Double

    public fun isNaN(value: Double): Boolean
}
@file:JvmVersion
package kotlin.comparisons

import kotlin.internal.InlineOnly

public typealias Comparator<T> = java.util.Comparator<T>

// shouldn't be needed here, KT-13513
@InlineOnly
public inline fun <T> Comparator(crossinline comparison: (T, T) -> Int): Comparator<T> = object : Comparator<T> {
    override fun compare(obj1: T, obj2: T): Int = comparison(obj1, obj2)
}

package kotlin

import java.util.*

/** Returns a new read-only list of given elements */
public fun listOf<T>(vararg values: T): List<T> = arrayListOf(*values)

/** Returns a new read-only set of given elements */
public fun setOf<T>(vararg values: T): Set<T> = values.toCollection(LinkedHashSet<T>())

/** Returns a new read-only map of given pairs, where the first value is the key, and the second is value */
public fun mapOf<K, V>(vararg values: Pair<K, V>): Map<K, V> = hashMapOf(*values)

/** Returns a new ArrayList with a variable number of initial elements */
public fun arrayListOf<T>(vararg values: T) : ArrayList<T> = values.toCollection(ArrayList<T>(values.size))

deprecated("Use listOf(...) or arrayListOf(...) instead")
public fun arrayList<T>(vararg values: T) : ArrayList<T> = arrayListOf(*values)

/**
 * Returns a new [[HashMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createUsingPairs
 */
public fun <K,V> hashMapOf(vararg values: Pair<K,V>): HashMap<K,V> {
    val answer = HashMap<K,V>(values.size)
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v.first, v.second)
    }
    return answer
}

deprecated("Use mapOf(...) or hashMapOf(...) instead")
public fun <K,V> hashMap(vararg values: Pair<K,V>): HashMap<K,V> = hashMapOf(*values)

/** Returns the size of the collection */
val Collection<*>.size : Int
    get() = size()

/** Returns true if this collection is empty */
val Collection<*>.empty : Boolean
    get() = isEmpty()

val Collection<*>.indices : IntRange
    get() = 0..size-1

val Int.indices: IntRange
    get() = 0..this-1

/** Returns true if the collection is not empty */
public inline fun <T> Collection<T>.notEmpty() : Boolean = !this.isEmpty()

/** Returns the Collection if its not null otherwise it returns the empty list */
public inline fun <T> Collection<T>?.orEmpty() : Collection<T>
    = if (this != null) this else Collections.emptyList<T>() as Collection<T>


/** TODO these functions don't work when they generate the Array<T> versions when they are in JLIterables */
public inline fun <T: Comparable<T>> Iterable<T>.toSortedList() : List<T> = toCollection(ArrayList<T>()).sort()

public inline fun <T: Comparable<T>> Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> = toList().sort(comparator)


// List APIs

/** Returns the List if its not null otherwise returns the empty list */
public inline fun <T> List<T>?.orEmpty() : List<T>
    = if (this != null) this else Collections.emptyList<T>() as List<T>

/**
  TODO figure out necessary variance/generics ninja stuff... :)
public inline fun <in T> List<T>.sort(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val comparator = java.util.Comparator<T>() {
    public fun compare(o1: T, o2: T): Int {
      val v1 = transform(o1)
      val v2 = transform(o2)
      if (v1 == v2) {
        return 0
      } else {
        return v1.compareTo(v2)
      }
    }
  }
  answer.sort(comparator)
}
*/

/**
 * Returns the first item in the list
 *
 * @includeFunctionBody ../../test/ListTest.kt first
 */
val <T> List<T>.first : T?
    get() = this.head


/**
 * Returns the last item in the list
 *
 * @includeFunctionBody ../../test/ListTest.kt last
 */
val <T> List<T>.last : T?
    get() {
      val s = this.size
      return if (s > 0) this.get(s - 1) else null
    }

/**
 * Returns the index of the last item in the list or -1 if the list is empty
 *
 * @includeFunctionBody ../../test/ListTest.kt lastIndex
 */
val <T> List<T>.lastIndex : Int
    get() = this.size - 1

/**
 * Returns the first item in the list
 *
 * @includeFunctionBody ../../test/ListTest.kt head
 */
val <T> List<T>.head : T?
    get() = this.get(0)

/**
 * Returns all elements in this collection apart from the first one
 *
 * @includeFunctionBody ../../test/ListTest.kt tail
 */
val <T> List<T>.tail : List<T>
    get() {
        return this.drop(1)
    }

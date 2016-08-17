/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlin.collections

import kotlin.comparisons.*


@library("copyToArray")
public fun <reified T> Collection<T>.toTypedArray(): Array<T> = noImpl


/**
 * Returns an immutable list containing only the specified object [element].
 */
public fun <T> listOf(element: T): List<T> = arrayListOf(element)

/**
 * Returns an immutable set containing only the specified object [element].
 */
public fun <T> setOf(element: T): Set<T> = hashSetOf(element)

/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.
 */
public fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = hashMapOf(pair)


// constructor-like functions:

public fun <E> HashSet(c: Collection<E>): HashSet<E>
        = HashSet<E>(c.size).apply { addAll(c) }

public fun <E> LinkedHashSet(c: Collection<E>): HashSet<E>
        = LinkedHashSet<E>(c.size).apply { addAll(c) }

public fun <K, V> HashMap(m: Map<out K, V>): HashMap<K, V>
        = HashMap<K, V>(m.size).apply { putAll(m) }

public fun <K, V> LinkedHashMap(m: Map<out K, V>): LinkedHashMap<K, V>
        = LinkedHashMap<K, V>(m.size).apply { putAll(m) }

public fun <E> ArrayList(c: Collection<E>): ArrayList<E>
        = ArrayList<E>().apply { asDynamic().array = c.toTypedArray<Any?>() } // black dynamic magic




internal object Collections {
    internal fun <T: Comparable<T>> sort(list: MutableList<T>): Unit = kotlin.collections.sort(list, naturalOrder())

    internal fun <T> sort(list: MutableList<T>, comparator: Comparator<in T>): Unit = kotlin.collections.sort(list, comparator)

    internal fun <T> reverse(list: MutableList<T>): Unit {
        val size = list.size
        for (i in 0..(size / 2) - 1) {
            val i2 = size - i - 1
            val tmp = list[i]
            list[i] = list[i2]
            list[i2] = tmp
        }
    }
}

@library("collectionsMax")
private fun <T> max(col: Collection<T>, comp: Comparator<in T>): T = noImpl

@library("collectionsSort")
private fun <T> sort(list: MutableList<T>, comparator: Comparator<in T>): Unit = noImpl

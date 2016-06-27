@file:kotlin.jvm.JvmName("TypeAliasesKt")
@file:kotlin.jvm.JvmMultifileClass

package kotlin.collections

typealias RandomAccess = java.util.RandomAccess
typealias Comparator<T> = java.util.Comparator<T> // kotlin.comparisons?

// internal?
internal typealias ArrayList<T> = java.util.ArrayList<T>
internal typealias LinkedHashMap<K, V> = java.util.LinkedHashMap<K, V>
internal typealias HashMap<K, V> = java.util.HashMap<K, V>
internal typealias LinkedHashSet<T> = java.util.LinkedHashSet<T>
internal typealias HashSet<T> = java.util.HashSet<T>
internal typealias AbstractList<T> = java.util.AbstractList<T>

internal typealias Collections = java.util.Collections

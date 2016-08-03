package kotlin.collections


public interface Grouping<T, out K> {
    fun elementIterator(): Iterator<T>
    fun keySelector(element: T): K
}


public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.aggregateTo(destination: M, operation: (key: K, value: R?, element: T, first: Boolean) -> R): M {
    for (e in this.elementIterator()) {
        val key = keySelector(e)
        val value = destination[key]
        destination[key] = operation(key, value, e, value == null && !destination.containsKey(key))
    }
    return destination
}

public inline fun <T, K, R> Grouping<T, K>.aggregate(operation: (key: K, value: R?, element: T, first: Boolean) -> R): Map<K, R> {
    val result = mutableMapOf<K, R>()
    for (e in this.elementIterator()) {
        val key = keySelector(e)
        val value = result[key]
        result[key] = operation(key, value, e, value == null && !result.containsKey(key))
    }
    return result
}

public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(destination: M, initialValueSelector: (K, T) -> R, operation: (K, R, T) -> R): M =
        aggregateTo(destination) { key, value, e, first -> operation(key, if (first) initialValueSelector(key, e) else value as R, e) }

public inline fun <T, K, R> Grouping<T, K>.fold(initialValueSelector: (K, T) -> R, operation: (K, R, T) -> R): Map<K, R> =
        aggregate { key, value, e, first -> operation(key, if (first) initialValueSelector(key, e) else value as R, e) }


public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(destination: M, initialValue: R, operation: (R, T) -> R): M =
        aggregateTo(destination) { k, v, e, first -> operation(if (first) initialValue else v as R, e) }


public inline fun <T, K, R> Grouping<T, K>.fold(initialValue: R, operation: (R, T) -> R): Map<K, R> =
        aggregate { k, v, e, first -> operation(if (first) initialValue else v as R, e) }


public inline fun <S, T : S, K, M : MutableMap<in K, S>> Grouping<T, K>.reduceTo(destination: M, operation: (K, S, T) -> S): M =
        aggregateTo(destination) { key, value, e, first ->
            if (first) e else operation(key, value as S, e)
        }

public inline fun <S, T : S, K> Grouping<T, K>.reduce(operation: (K, S, T) -> S): Map<K, S> =
        aggregate { key, value, e, first ->
            if (first) e else operation(key, value as S, e)
        }


public fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.countEachTo(destination: M): M = foldTo(destination, 0) { acc, e -> acc + 1 }

// TODO: optimize allocations with IntRef
public fun <T, K> Grouping<T, K>.countEach(): Map<K, Int> = fold(0) { acc, e -> acc + 1 }

public inline fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.sumEachByTo(destination: M, valueSelector: (T) -> Int): M =
        foldTo(destination, 0) { acc, e -> acc + valueSelector(e)}

// TODO: optimize with IntRef
public inline fun <T, K> Grouping<T, K>.sumEachBy(valueSelector: (T) -> Int): Map<K, Int> =
        fold(0) { acc, e -> acc + valueSelector(e)}


/*

// by long and by double overloads

public inline fun <T, K, M : MutableMap<in K, Long>> Grouping<T, K>.sumEachByLongTo(destination: M, valueSelector: (T) -> Long): M =
        foldTo(destination, 0L) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K> Grouping<T, K>.sumEachByLong(valueSelector: (T) -> Long): Map<K, Long> =
        fold(0L) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K, M : MutableMap<in K, Double>> Grouping<T, K>.sumEachByDoubleTo(destination: M, valueSelector: (T) -> Double): M =
        foldTo(destination, 0.0) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K> Grouping<T, K>.sumEachByDouble(valueSelector: (T) -> Double): Map<K, Double> =
        fold(0.0) { acc, e -> acc + valueSelector(e)}*/

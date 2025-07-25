// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class Inv<T>

class A
class B

fun <K> select(x: K, y: K): K = x
fun <V> generic(x: Inv<V>) {}

fun test1(a: Inv<A>, b: Inv<B>) {
    generic(select(a, b))
}

fun test2(a: Inv<*>?, b: Inv<*>) {
    generic(a ?: b)
    generic(if (a != null) a else b)
    generic(a!!)
}

fun test3(a: Inv<out Any>, b: Inv<out Any>) {
    generic(select(a, b))
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, elvisExpression, equalityExpression,
functionDeclaration, ifExpression, nullableType, outProjection, smartcast, starProjection, typeParameter */

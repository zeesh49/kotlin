// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.*

fun test(list: ArrayList<Int>, comparatorFun: (Int, Int) -> Int) {
    sort(list, Comparator(comparatorFun))
}


public fun <E> sort(list: List<E>, c: Comparator<in E>) {
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inProjection, nullableType, typeParameter */

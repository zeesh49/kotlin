// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(x: T, y: T) {}
}

fun test(a: A<out CharSequence>) {
    a.foo(<!TYPE_MISMATCH!>""<!>, <!TYPE_MISMATCH!>""<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, outProjection, stringLiteral, typeParameter */

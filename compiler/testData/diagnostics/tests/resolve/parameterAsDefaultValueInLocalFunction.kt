// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    fun foo(x: Int = <!UNINITIALIZED_PARAMETER!>x<!>) {}
    fun bar(x: String, y: String = x) {}
    fun baz(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, localFunction */

// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-70208
// WITH_STDLIB
// Notes: it will be relevant after merging stdlib K2 branch when `kotlin.Boolean` becomes `expect` class

fun checkExpectBooleanIsExhaustive(b: Boolean): Int {
    return when (b) {
        true -> 1
        false -> 2
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, smartcast, whenExpression,
whenWithSubject */

// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// FILE: a.kt

package a.b.c

class D {
    fun foo() = 42
}

// FILE: b.kt

import kotlin.reflect.KFunction1

fun main() {
    val x = a.b.c.D::foo

    checkSubtype<KFunction1<a.b.c.D, Int>>(x)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, integerLiteral, localProperty, nullableType, propertyDeclaration, typeParameter,
typeWithExtension */

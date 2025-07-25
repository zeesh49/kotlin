// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-51017

interface A
interface B : A

fun <V : A> V.foo(): V = this
fun <T : B> T.foo(): T = this

fun test(list: List<B>) {
    B::foo // No ambiguity, T.foo wins
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
thisExpression, typeConstraint, typeParameter */

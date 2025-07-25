// RUN_PIPELINE_TILL: FRONTEND
package Hello

open class Base<T>
class StringBase : Base<String>()

class Client<T, X: Base<T>>(x: X)

fun test() {
    val c = Client(StringBase()) // Type inference fails here for T.
    val i : Int = <!INITIALIZER_TYPE_MISMATCH!>c<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter */

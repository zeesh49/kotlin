// RUN_PIPELINE_TILL: FRONTEND
import Outer.Inner

open class Outer {
    open inner class Inner
}

class Test : <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER, SUPERTYPE_NOT_INITIALIZED!>Inner<!> {
    fun foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner */

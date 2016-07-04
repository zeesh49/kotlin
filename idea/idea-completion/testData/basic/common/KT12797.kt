abstract class A {
    inner class InnerInA
}

abstract class B : A() {
    inner class InnerInB
}

fun foo(b: B) {
    val v = b.Inner<caret>
}

// EXIST: InnerInA
// EXIST: InnerInB
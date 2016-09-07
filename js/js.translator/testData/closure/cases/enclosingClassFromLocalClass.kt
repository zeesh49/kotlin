package foo

open class A(private val x: String) {
    fun foo(): String {
        class B : A("fail") {
            fun bar() = x
        }
        return B().bar()
    }
}

fun box() = A("OK").foo()
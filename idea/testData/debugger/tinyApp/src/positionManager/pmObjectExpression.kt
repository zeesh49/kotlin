package pmObjectExpression

class A {
    fun foo() {
        object {
            fun bar() {
                //Breakpoint!
                ""   // pmObjectExpression.A\$foo\$1
            }
        }.bar()
    }
}

fun main(args: Array<String>) {
    A().foo()
}

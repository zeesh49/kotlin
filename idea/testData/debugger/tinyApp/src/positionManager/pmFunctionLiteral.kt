package pmFunctionLiteral

class A {
    fun foo() {
        {
            fun innerFoo() {
                //Breakpoint!
                ""   // pmFunctionLiteral.A\$foo\$1\$1
            }
            innerFoo()
        }()
    }
}

fun main(args: Array<String>) {
    A().foo()
}

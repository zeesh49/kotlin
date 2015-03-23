package pmFunctionLiteralInVal

class A {
    fun foo() {
        val a = {
            fun innerFoo() {
                //Breakpoint!
                val b = {}   // pmFunctionLiteralInVal.A\$foo\$a\$1\$1
            }
            innerFoo()
        }()
    }
}


fun main(args: Array<String>) {
    A().foo()
}
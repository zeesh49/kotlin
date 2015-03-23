package pmAnonymousFunction

class A {
    fun foo() {
        {
            //Breakpoint!
            ""   // pmAnonymousFunction.A\$foo\$1
        }()
    }
}

fun main(args: Array<String>) {
    A().foo()
}

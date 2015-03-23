package pmAnonymousNamedFunction

fun foo() {
    val lambda = {
        //Breakpoint!
        val a = 1 // pmAnonymousNamedFunction.PmAnonymousNamedFunctionPackage\$pmAnonymousNamedFunction\$.+\$foo\$lambda\$1
    }()
}

fun main(args: Array<String>) {
    foo()
}

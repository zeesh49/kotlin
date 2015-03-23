package pmLocalFunction

fun foo(): String {
    fun bar(): String {
        //Breakpoint!
        return ""   // pmLocalFunction.PmLocalFunctionPackage\$pmLocalFunction\$.+\$foo\$1
    }
    return bar()
}

fun main(args: Array<String>) {
    foo()
}

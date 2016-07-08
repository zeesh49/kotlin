package ifCapturedVariableKt9118

fun main(args: Array<String>) {
    //Breakpoint!
    if (args.size < 0) {
        println()
    }
    var isCompiledDataFromCache = true
    foo {
        isCompiledDataFromCache = false
    }
}

fun foo(f: () -> Unit) {
    f()
}
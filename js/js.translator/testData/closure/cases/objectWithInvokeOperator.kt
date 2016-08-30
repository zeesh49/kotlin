package foo

object O {
    val result = "OK"
}

operator fun O.invoke() = result

fun f() = { O() }

fun box(): String {
    return f()()
}
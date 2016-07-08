enum class A {
    ONE
}

fun box(): String {
    if (A.ONE == A.ONE) "1" else "2"
    return "OK"
}



// 1 LDC "1"
// 1 LDC "2"
// 1 IFEQ
// 1 IF
// 1 GOTO

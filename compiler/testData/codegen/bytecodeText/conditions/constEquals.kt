const val z = 1

object A {
    const val z = 1
}

fun box(): String {
    if (A.z == z) "1" else "2"
    return "OK"
}



// 1 LDC "1"
// 1 LDC "2"
// 1 IF_ICMPNE
// 1 IF
// 1 GOTO

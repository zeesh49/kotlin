const val z = false
object A {
    const val z2 = true
}

fun box(): String {
    if (z || A.z2) "1" else "2"
    return "OK"
}


// 1 LDC "1"
// 1 LDC "2"
// 1 IFEQ
// 1 IFNE
// 2 IF
// 1 GOTO

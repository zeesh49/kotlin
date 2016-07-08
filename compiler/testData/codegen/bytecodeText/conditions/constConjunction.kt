object A {
    val z = true
    const val z2 = true
}

fun box(): String {
    if (A.z && A.z2) "1" else "2"
    return "OK"
}


// 1 ICONST_1
// 1 LDC "1"
// 1 LDC "2"
// 2 IFEQ
// 2 IF
// 1 GOTO

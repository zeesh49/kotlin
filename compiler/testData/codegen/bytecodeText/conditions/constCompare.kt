const val one = 1
fun box(): String {
    if (one == 1) "1" else "2"
    return "OK"
}


// 1 ICONST_1
// 1 ICONST_1
// 1 LDC "1"
// 1 LDC "2"
// 1 IF_ICMPNE
// 1 IF
// 1 GOTO

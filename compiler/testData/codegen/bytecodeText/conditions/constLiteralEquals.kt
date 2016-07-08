fun box(): String {
    if (1 == 1) "1" else "2"
    return "OK"
}


// 0 ICONST_1
// 1 LDC "1"
// 0 LDC "2"
// 0 IF
// 0 GOTO

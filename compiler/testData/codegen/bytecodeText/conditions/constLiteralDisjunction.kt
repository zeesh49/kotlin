fun box(): String {
    if (false || true) "1" else "2"
    return "OK"
}


// 0 ICONST_1
// 0 ICONST_0
// 1 LDC "1"
// 0 LDC "2"
// 0 IF
// 0 GOTO

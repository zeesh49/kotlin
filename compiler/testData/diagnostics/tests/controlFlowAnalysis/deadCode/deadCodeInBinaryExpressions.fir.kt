// RUN_PIPELINE_TILL: BACKEND
fun testBinary1() {
    operator fun Int.times(s: String) {}

    todo() <!UNREACHABLE_CODE!>* ""<!>
}
fun testBinary2() {
    "1" <!UNREACHABLE_CODE!>+<!> todo()
}

fun testElvis1() {
    todo() <!UNREACHABLE_CODE, USELESS_ELVIS!>?: ""<!>
}

fun testElvis2(s: String?) {
    s ?: todo()

    bar()
}

fun testAnd1(b: Boolean) {
    b && todo()

    bar()
}

fun testAnd2(b: Boolean) {
    todo() && <!UNREACHABLE_CODE!>b<!>
}

fun returnInBinary1(): Boolean {
    (return true) && (<!UNREACHABLE_CODE!>return false<!>)
}

fun returnInBinary2(): Boolean {
    (return true) || (<!UNREACHABLE_CODE!>return false<!>)
}

fun todo(): Nothing = throw Exception()
fun bar() {}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, disjunctionExpression, elvisExpression,
funWithExtensionReceiver, functionDeclaration, localFunction, multiplicativeExpression, nullableType, operator,
stringLiteral */

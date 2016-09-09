// "Replace 'try' expression with assignment" "true"
// WITH_RUNTIME

fun foo() {
    val x: Int
    try {
        x = 1
    }
    catch (e: Exception) {
        <caret>x = 2
    }
}
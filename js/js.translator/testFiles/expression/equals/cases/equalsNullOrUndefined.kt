package foo

fun box() : Boolean {
    val a: Int? = null
    val r = a == null
    if (!r || a != null) return false

    var i = 0;
    fun foo(): Int? = ++i;
    if (foo() == null || i != 1) return false

    return true
}
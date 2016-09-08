// See KT-13765: no smart cast at hashCode()

fun foo(flag: Boolean) {
    var x: String? = null

    if (x == null) {
        x = if (flag) "34" else "12"
    }

    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
}
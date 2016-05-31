var result = "fail"
fun concat(a: Long, s: String, z: String) {
    result = "" + a + s + z
}

private fun String.doWork(other: String?) {
    concat(1L, this, if (other == null) return else other)
}

fun box(): String {
    "O".doWork("K")
    return if (result != "1OK") "fail" else "OK"
}
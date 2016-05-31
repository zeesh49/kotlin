fun concat(a: String, b: String, c: String): Long {
    return (a + b + c).length.toLong()
}

private fun String.doWork(other: String, other2: String?): Long {
    return concat(this, other, if (other2 == null) return 1L else other2)
}

fun box(): String {
    return if ("O".doWork("K", "11") == 4L) "OK" else "fail"
}
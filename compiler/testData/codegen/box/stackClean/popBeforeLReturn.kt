fun concat(a: String, b: String): Long {
    return (a + b).length.toLong()
}

private fun String.doWork(other: String?): Long {
    return concat(this, if (other == null) return 1L else other)
}

fun box(): String {
    return if ("O".doWork("K") == 2L) "OK" else "fail"
}
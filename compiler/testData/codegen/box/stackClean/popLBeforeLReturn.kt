fun concat(a: Long, c: String): Long {
    return (c + a).length.toLong()
}

private fun doWork(l: Long, other2: String?): Long {
    return concat(l, if (other2 == null) return 1L else other2)
}

fun box(): String {
    return if (doWork(1L, "OK1") == 4L) "OK" else "fail"
}
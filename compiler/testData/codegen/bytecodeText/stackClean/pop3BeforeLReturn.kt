fun concat(a: String, b: String, c: String, d: String): Long {
    return (a + b + c + d).length.toLong()
}

private fun String.doWork(other: String, other2: String?): Long {
    return concat("X", this, other, if (other2 == null) return 1L else other2)
}

fun box(): String {
    return if ("O".doWork("K", "11") == 5L) "O1K" else "fail"
}

// 1 LSTORE 3\s+POP2\s+POP\s+LLOAD 3\s+LRETURN
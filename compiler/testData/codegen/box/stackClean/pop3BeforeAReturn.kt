fun concat(a: String, b: String, c: String, d: String): String {
    return a + b + c + d
}

private fun String.doWork(other: String, other2: String?): String {
    return concat("", this, other, if (other2 == null) return "null" else other2)
}

fun box(): String {
    return "O".doWork("K", "")
}
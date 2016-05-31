var result = "fail"
fun concat(s: String, z: String) {
    result = s + z
}

private fun String.doWork(other: String?) {
    concat(this, if (other == null) return else other)
}

fun box(): String {
    "O".doWork("K")
    return result
}

// 1 POP\s+RETURN
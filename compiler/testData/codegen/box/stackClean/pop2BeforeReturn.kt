var result = "fail"
fun concat(a: String, s: String, z: String) {
    result = a + s + z
}

private fun String.doWork(other: String?) {
    concat("", this, if (other == null) return else other)
}

fun box(): String {
    "O".doWork("K")
    return result
}
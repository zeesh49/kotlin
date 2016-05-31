fun concat(s: String, z: String): String {
    return s + z
}

private fun String.doWork(other: String?): String {
    return concat(this, if (other == null) return "" else other)
}

fun box(): String {
    return "O".doWork("K")
}

// 1 SWAP\s+POP\s+ARETURN
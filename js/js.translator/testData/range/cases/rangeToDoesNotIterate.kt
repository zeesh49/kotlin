package foo


fun box(): Boolean {
    for (i in 0.rangeTo(-1)) {
        return false
    }
    return true
}
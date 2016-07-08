var field: Int = 0

fun next(): Int {
    return ++field
}


fun box(): String {
    val task: String

    while(1 < 2) {
        if (next() % 2 == 0) {
            task = "OK"
            break
        }
    }

    return task
}

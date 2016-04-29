fun foo(actions: List<(String) -> Unit>, p1: String, p2: Int) {
    actions[0](<caret>)
}

// EXIST: p1
// EXIST: p2

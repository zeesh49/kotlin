fun foo() {
    buildString {
        with(xxx) {
            with(f()) {
                g()?.let {
                    listOf(1, 2).filterTo(collection) { item ->
                        doIt() {
                            val v = { p: Int ->
                                x(1, { <caret> })
                            }
                        }
                    }
                }
            }
        }
    }
}
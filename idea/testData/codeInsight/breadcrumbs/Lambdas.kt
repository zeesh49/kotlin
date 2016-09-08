fun foo() {
    buildString {
        with(xxx) {
            with(f()) {
                g()?.let {
                    listOf(1, 2, 3, 4, 5).filterTo(collection) { item ->
                        doIt() {
                            val v = { p: Int ->
                                x(1, { Something().apply { <caret> } })
                            }
                        }
                    }
                }
            }
        }
    }
}
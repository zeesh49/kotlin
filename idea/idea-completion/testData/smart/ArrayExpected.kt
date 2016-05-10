fun foo(): Array<Any> {
    return listOf("a").<caret>
}

// EXIST: toTypedArray

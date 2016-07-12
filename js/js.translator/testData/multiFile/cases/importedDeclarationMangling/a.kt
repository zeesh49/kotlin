package foo

import bar.*
import bar.Some.importedFunc

fun box(): String {
    Some.justFunc()
    importedFunc()
    assertEquals("justFunc();importedFunc();", log)
    return "OK"
}

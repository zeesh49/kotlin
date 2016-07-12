package bar

var log = ""

object Some {
    fun justFunc() {
        log += "justFunc();"
    }

    fun importedFunc() {
        log += "importedFunc();"
    }
}
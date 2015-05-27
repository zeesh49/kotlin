import test.*

fun box(): String {
    val samePackageResult = samePackageCall()
    if (samePackageResult != "OK") return "same package inline fail: $samePackageResult"

    val sameClassResult = A().sameClassCall()
    if (sameClassResult != "OK") return "same class inline fail: $sameClassResult"

    return A().classInline { a, b -> a + b }
}

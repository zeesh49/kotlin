package kotlin
// This file should be excluded from tests using StdLib, as these methods conflict with corresponding methods from kotlin.test
// see StdLibTestBase.removeAdHocAssertions

fun fail(message: String? = null): Nothing = js("throw new Error(message)")

fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (!equal(expected, actual)) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        fail("Unexpected value:$msg expected = '$expected', actual = '$actual'")
    }
}

fun <T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    if (equal(illegal, actual)) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        fail("Illegal value:$msg illegal = '$illegal', actual = '$actual'")
    }
}

private fun equal(a: Any?, b: Any?): Boolean {
    if (a is Array<*> && b is Array<*>) {
        if (a.size != b.size) return false
        for (i in 0..(a.size - 1)) {
            if (!equal(a[i], b[i])) return false
        }
        return true
    } else {
        return a == b
    }
}

fun assertTrue(actual: Boolean, message: String? = null) = assertEquals(true, actual, message)

fun assertFalse(actual: Boolean, message: String? = null) = assertEquals(false, actual, message)

fun testTrue(f: () -> Boolean) {
    assertTrue(f(), f.toString())
}

fun testFalse(f: () -> Boolean) {
    assertFalse(f(), f.toString())
}

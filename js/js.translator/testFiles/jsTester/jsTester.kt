package kotlin.test

import kotlin.test.*
import js.*

public var asserter : Asserter = JsTestsAsserter()

public class JsTestsAsserter(): Asserter {
    public override fun assertTrue(message: String, actual: Boolean) {
        assert(actual, message)
    }
    public override fun assertEquals(message: String, expected: Any?, actual: Any?) {
        assert(specialEquals(expected, actual) || actual == expected, "$message. Expected <$expected> actual <$actual>")
    }
    public override fun assertNotNull(message: String, actual: Any?) {
        assert(actual != null, message)
    }
    public override fun assertNull(message: String, actual: Any?) {
        assert(actual == null, message)
    }
    public override fun fail(message: String) {
        assert(false, message)
    }
}

native("JsTests.assert")
public fun assert(value: Boolean, message: String): Unit = js.noImpl

// todo fix workaround
native("JsTests.specialEquals")
public fun specialEquals(expected: Any?, actual: Any?): Boolean = js.noImpl
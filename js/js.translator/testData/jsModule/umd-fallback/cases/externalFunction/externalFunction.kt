package foo

@JsModule("libfoo") @JsNonModule @native fun foo(x: Int): Int = noImpl

@JsModule("libbar") @JsNonModule @native("baz") fun bar(x: Int): Int = noImpl

fun box(): String {
    assertEquals(65, foo(42))
    assertEquals(142, bar(100))
    return "OK"
}
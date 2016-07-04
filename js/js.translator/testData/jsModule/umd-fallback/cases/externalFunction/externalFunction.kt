package foo

@JsModule("lib") @JsNonModule @native fun foo(x: Int): Int = noImpl

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}
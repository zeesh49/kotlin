@file:JsModule("lib")
package foo

@native class A(@native val x: Int = noImpl) {
    @native fun foo(y: Int): Int = noImpl
}

@native object B {
    @native val x: Int = noImpl

    @native fun foo(y: Int): Int = noImpl
}

@native fun foo(y: Int): Int = noImpl

@native val bar: Int = noImpl

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    assertEquals(123, B.x)
    assertEquals(265, B.foo(142))

    /* TODO: get rid of usages of getQualifier, then uncomment it (it's already fixed by js-name branch)
    assertEquals(365, foo(23))
    assertEquals(423, bar)*/

    return "OK"
}
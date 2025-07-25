// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

class Foo {
    fun foo(a: Foo): Foo = a
    var f: Foo? = null
}

fun main() {
    val x: Foo? = null
    val y: Foo? = null

    x<!UNSAFE_CALL!>.<!>foo(<!TYPE_MISMATCH!>y<!>)
    x!!.foo(<!TYPE_MISMATCH!>y<!>)
    x.foo(y!!)
    x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo(y<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)

    val a: Foo? = null
    val b: Foo? = null
    val c: Foo? = null

    a<!UNSAFE_CALL!>.<!>foo(b<!UNSAFE_CALL!>.<!>foo(<!TYPE_MISMATCH!>c<!>))
    a!!.foo(b<!UNSAFE_CALL!>.<!>foo(<!TYPE_MISMATCH!>c<!>))
    a.foo(b!!.foo(<!TYPE_MISMATCH!>c<!>))
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo(b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo(<!TYPE_MISMATCH!>c<!>))
    a.foo(b.foo(c!!))
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo(b.foo(c<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>))
    a.foo(b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo(c<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>))
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo(b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo(c<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>))

    val z: Foo? = null
    z!!.foo(z<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)

    val w: Foo? = null
    w<!UNSAFE_CALL!>.<!>f = z
    (w<!UNSAFE_CALL!>.<!>f) = z
    (label@ w<!UNSAFE_CALL!>.<!>f) = z
    w!!.f = z
    w.f = z
    w<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.f = z
    <!SMARTCAST_IMPOSSIBLE!>w.f<!>.f = z
    w.f!!.f = z
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, functionDeclaration, localProperty, nullableType,
propertyDeclaration, smartcast */

// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.java
public enum A {
    ENTRY("1"),
    ANOTHER("2");

    public String s;

    private A(String s) {
        this.s = s;
    }
}

// FILE: test.kt

fun main() {
    val c = A.ENTRY
    c.s
    A.ANOTHER.s
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaProperty, javaType, localProperty, propertyDeclaration */

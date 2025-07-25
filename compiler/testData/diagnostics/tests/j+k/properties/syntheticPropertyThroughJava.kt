// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-59550
// FILE: Base.kt
abstract class Base(private val foo: String) {
    fun getFoo(): String = foo
}

// FILE: Intermediate.java
public class Intermediate extends Base {
    public Intermediate(String foo) {
        super(foo);
    }
}

// FILE: main.kt
class Final(val i: Intermediate) : Intermediate(i.foo)

fun test(x: Final) {
    x.foo
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaProperty, javaType, primaryConstructor,
propertyDeclaration */

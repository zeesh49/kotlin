// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// CHECK_TYPE

// FILE: Foo.java
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class Foo {
    interface FObject {
        void invoke(Object i);
    }

    public String test(FObject f) { return ""; }
    public int test(Function1<Integer, Unit> f) { return 1; }
}

// FILE: 1.kt
fun bar() {
    Foo().test {} checkType { _<String>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, javaFunction, javaType, lambdaLiteral, nullableType, samConversion, typeParameter, typeWithExtension */

// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A {
    enum class E {
        ENTRY
    }
    
    companion object {
    }
}



class B {
    companion object {
    }
    
    enum class E {
        ENTRY
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, nestedClass, objectDeclaration */

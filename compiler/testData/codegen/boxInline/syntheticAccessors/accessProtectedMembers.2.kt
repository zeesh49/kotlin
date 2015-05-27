package test

fun samePackageCall(): String {
    return A().classInline { s, s2 -> s + s2 }
}

class A {

    protected val prop: String = "O"

    protected fun callFun(): String = "K"

    inline fun classInline(p: (String, String) -> String): String {
        return p(prop, callFun())
    }

    fun sameClassCall(): String {
        return classInline { s, s2 -> s + s2 }
    }
}


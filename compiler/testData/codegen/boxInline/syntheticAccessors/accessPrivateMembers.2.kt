package test

private val packageProp = "O"

private fun packageFun() = "K"

inline fun packageInline(p: (String, String) -> String): String {
    return p(packageProp, packageFun())
}

fun samePackageCall(): String {
    return packageInline { s, s2 -> s + s2 }
}

class A {

    private val prop = "O"

    private fun callFun() = "K"

    inline fun classInline(p: (String, String) -> String): String {
        return p(prop, callFun())
    }

    fun sameClassCall(): String {
        return classInline { s, s2 -> s + s2 }
    }
}


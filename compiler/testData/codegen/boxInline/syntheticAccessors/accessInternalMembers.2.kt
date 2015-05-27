package test

internal val packageProp = "O"

internal fun packageFun() = "K"

inline fun packageInline(p: (String, String) -> String): String {
    return p(packageProp, packageFun())
}

fun samePackageCall(): String {
    return packageInline { s, s2 -> s + s2 }
}

class A {

    internal val prop = "O"

    internal fun callFun() = "K"

    inline fun classInline(p: (String, String) -> String): String {
        return p(prop, callFun())
    }

    fun sameClassCall(): String {
        return classInline { s, s2 -> s + s2 }
    }
}


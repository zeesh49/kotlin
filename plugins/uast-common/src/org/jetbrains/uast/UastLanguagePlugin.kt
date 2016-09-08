package org.jetbrains.uast

import com.intellij.psi.*

abstract class UastLanguagePlugin {
    open lateinit var context: UastContext

    /**
     * Checks if the file with the given [fileName] is supported.
     *
     * @param fileName the source file name.
     * @return true, if the file is supported by this converter, false otherwise.
     */
    abstract fun isFileSupported(fileName: String): Boolean
    
    /**
     * Returns the converter priority. Might be positive, negative or 0 (Java's is 0).
     * UastConverter with the higher priority will be queried earlier.
     *
     * Priority is useful when a language N wraps its own elements (NElement) to, for example, Java's PsiElements,
     *  and Java resolves the reference to such wrapped PsiElements, not the original NElement.
     * In this case N implementation can handle such wrappers in UastConverter earlier than Java's converter,
     *  so N language converter will have a higher priority.
     */
    abstract val priority: Int

    abstract fun convertElement(element: Any?, parent: UElement?, requiredType: Class<out UElement>? = null): UElement?

    /**
     * Convert [element] to the [UElement] with the given parent.
     */
    abstract fun convertElementWithParent(element: Any?, requiredType: Class<out UElement>?): UElement?

    abstract fun getMethodCallExpression(
            e: PsiElement, 
            containingClassFqName: String?, 
            methodName: String
    ): Pair<UCallExpression, PsiMethod>?

    abstract fun getConstructorCallExpression(
            e: PsiElement,
            fqName: String
    ) : Triple<UCallExpression, PsiMethod, PsiClass>?

    open fun getMethodBody(e: PsiMethod): UExpression? {
        if (e is UMethod) return e.uastBody
        return (convertElementWithParent(e, null) as? UMethod)?.uastBody
    }

    open fun getInitializerBody(e: PsiClassInitializer): UExpression {
        if (e is UClassInitializer) return e.uastBody
        return (convertElementWithParent(e, null) as? UClassInitializer)?.uastBody ?: UastEmptyExpression
    }

    open fun getInitializerBody(e: PsiVariable): UExpression? {
        if (e is UVariable) return e.uastInitializer
        return (convertElementWithParent(e, null) as? UVariable)?.uastInitializer
    }
}

inline fun <reified T : UElement> UastLanguagePlugin.convertOpt(element: Any?, parent: UElement?): T? {
    return convertElement(element, parent) as? T
}

inline fun <reified T : UElement> UastLanguagePlugin.convert(element: Any?, parent: UElement?): T {
    return convertElement(element, parent, T::class.java) as T
}

inline fun <reified T : UElement> UastLanguagePlugin.convertWithParent(element: Any?): T? {
    return convertElementWithParent(element, T::class.java) as? T
}
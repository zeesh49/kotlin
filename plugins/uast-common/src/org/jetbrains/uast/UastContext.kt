package org.jetbrains.uast

import com.intellij.psi.*

abstract class UastContext : UastLanguagePlugin() {
    abstract val plugins: List<UastLanguagePlugin>
    
    init {
        this.context = this
    }

    private val sortedPlugins by lazy { plugins.sortedByDescending { it.priority } }

    override fun isFileSupported(fileName: String) = plugins.any { it.isFileSupported(fileName) }

    override val priority: Int
        get() = 0
    
    private inline fun <reified T : UDeclaration> getDeclaration(element: PsiElement, requiredType: Class<out UElement>?): T {
        for (plugin in sortedPlugins) {
            (plugin.convertElementWithParent(element, requiredType) as? T)?.let { return it }
        }
        error("Can't find language plugin for $element")
    }
    
    fun getMethod(method: PsiMethod): UMethod = getDeclaration(method, UMethod::class.java)
    
    fun getVariable(variable: PsiVariable): UVariable = getDeclaration(variable, UVariable::class.java)
    
    fun getClass(clazz: PsiClass): UClass = getDeclaration(clazz, UClass::class.java)

    override fun convertElement(element: Any?, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        for (plugin in sortedPlugins) {
            plugin.convertElement(element, parent, requiredType)?.let { return it }
        }
        return null
    }

    override fun convertElementWithParent(element: Any?, requiredType: Class<out UElement>?): UElement? {
        for (plugin in sortedPlugins) {
            plugin.convertElementWithParent(element, requiredType)?.let { return it }
        }
        return null
    }

    override fun getMethodCallExpression(e: PsiElement, containingClassFqName: String?, methodName: String): Pair<UCallExpression, PsiMethod>? {
        for (plugin in sortedPlugins) {
            plugin.getMethodCallExpression(e, containingClassFqName, methodName)?.let { return it }
        }
        return null
    }

    override fun getConstructorCallExpression(e: PsiElement, fqName: String): Triple<UCallExpression, PsiMethod, PsiClass>? {
        for (plugin in sortedPlugins) {
            plugin.getConstructorCallExpression(e, fqName)?.let { return it }
        }
        return null
    }

    override fun getMethodBody(e: PsiMethod): UExpression? {
        for (plugin in sortedPlugins) {
            plugin.getMethodBody(e)?.let { return it }
        }
        return null
    }

    override fun getInitializerBody(e: PsiVariable): UExpression? {
        for (plugin in sortedPlugins) {
            plugin.getInitializerBody(e)?.let { return it }
        }
        return null
    }

    override fun getInitializerBody(e: PsiClassInitializer): UExpression {
        for (plugin in sortedPlugins) {
            plugin.getInitializerBody(e)?.let { return it }
        }
        return UastEmptyExpression
    }
}

class UastContextImpl(override val plugins: List<UastLanguagePlugin>) : UastContext() {
    init {
        plugins.forEach { it.context = this }
    }
}
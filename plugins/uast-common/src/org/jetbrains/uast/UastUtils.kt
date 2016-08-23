@file:JvmMultifileClass
@file:JvmName("UastUtils")
package org.jetbrains.uast

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.expressions.UReferenceExpression

@JvmOverloads
fun <T : UElement> UElement.getParentOfType(parentClass: Class<out UElement>, strict: Boolean = true): T? {
    var element = (if (strict) containingElement else this) ?: return null
    while (true) {
        if (parentClass.isInstance(element)) {
            @Suppress("UNCHECKED_CAST")
            return element as T
        }
        element = element.containingElement ?: return null
    }
}

fun <T : UElement> UElement.getParentOfType(
        parentClass: Class<out UElement>, 
        strict: Boolean = true, 
        vararg terminators: Class<out UElement>
): T? {
    var element = (if (strict) containingElement else this) ?: return null
    while (true) {
        if (parentClass.isInstance(element)) {
            @Suppress("UNCHECKED_CAST")
            return element as T
        }
        if (terminators.any { it.isInstance(element) }) {
            return null
        }
        element = element.containingElement ?: return null
    }
}

fun <T : UElement> UElement.getParentOfType(strict: Boolean, vararg parentClasses: Class<out T>): T? {
    var element = (if (strict) containingElement else this) ?: return null
    while (true) {
        if (parentClasses.any { it.isInstance(element) }) {
            @Suppress("UNCHECKED_CAST")
            return element as T
        }
        element = element.containingElement ?: return null
    }
}

fun UElement.getContainingFile() = getParentOfType<UFile>(UFile::class.java)

fun UElement.getContainingUMethod() = getParentOfType<UMethod>(UMethod::class.java)
fun UElement.getContainingUVariable() = getParentOfType<UVariable>(UVariable::class.java)

fun UElement.getContainingMethod() = getContainingUMethod()?.psi
fun UElement.getContainingVariable() = getContainingUVariable()?.psi

fun PsiElement?.getContainingClass() = this?.let { PsiTreeUtil.getParentOfType(it, PsiClass::class.java) }

fun UElement.isChildOf(probablyParent: UElement, strict: Boolean = false): Boolean {
    tailrec fun isChildOf(current: UElement?, probablyParent: UElement): Boolean {
        return when (current) {
            null -> false
            probablyParent -> true
            else -> isChildOf(current.containingElement, probablyParent)
        }
    }

    return isChildOf(if (strict) this else containingElement, probablyParent)
}

/**
 * Resolves the receiver element if it implements [UResolvable].
 *
 * @return the resolved element, or null if the element was not resolved, or if the receiver element is not an [UResolvable].
 */
fun UElement.tryResolve(): PsiElement? = (this as? UResolvable)?.resolve()

fun UElement.tryResolveNamed(): PsiNamedElement? = (this as? UResolvable)?.resolve() as? PsiNamedElement

fun UElement.tryResolveUDeclaration(context: UastContext): UDeclaration? {
    return (this as? UResolvable)?.resolve()?.let { context.convertWithParent(it) as? UDeclaration }
}

fun UReferenceExpression?.getQualifiedName() = (this?.resolve() as? PsiClass)?.qualifiedName
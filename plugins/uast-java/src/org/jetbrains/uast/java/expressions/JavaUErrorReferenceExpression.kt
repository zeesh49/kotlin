package org.jetbrains.uast.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.expressions.UReferenceExpression

class JavaUErrorReferenceExpression(override val containingElement: UElement?) : UReferenceExpression {
    override fun resolve() = null
    override val resolvedName: String?
        get() = null
    override val isUsedAsExpression: Boolean
        get() = (containingElement as? UExpression)?.isUsedAsExpression ?: false
}
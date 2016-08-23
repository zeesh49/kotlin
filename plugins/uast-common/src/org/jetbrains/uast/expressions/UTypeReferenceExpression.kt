package org.jetbrains.uast.expressions

import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.name
import org.jetbrains.uast.visitor.UastVisitor

interface UTypeReferenceExpression : UExpression {
    val type: PsiType
    
    fun getQualifiedName() = PsiTypesUtil.getPsiClass(type)?.qualifiedName

    override fun accept(visitor: UastVisitor) {
        visitor.visitTypeReferenceExpression(this)
        visitor.afterVisitTypeReferenceExpression(this)
    }

    override fun logString() = "UTypeReferenceExpression (${type.name})"

    override fun renderString() = type.name
}
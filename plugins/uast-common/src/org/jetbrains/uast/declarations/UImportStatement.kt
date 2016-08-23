package org.jetbrains.uast

import com.intellij.psi.PsiElement
import org.jetbrains.uast.psi.PsiElementBacked
import org.jetbrains.uast.visitor.UastVisitor

interface UImportStatement : UResolvable, UElement, PsiElementBacked {
    val isOnDemand: Boolean
    val importReference: UElement?
    
    override fun logString() = "UImportStatement (onDemand = $isOnDemand)"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitImportStatement(this)) return
        visitor.afterVisitImportStatement(this)
    }
}
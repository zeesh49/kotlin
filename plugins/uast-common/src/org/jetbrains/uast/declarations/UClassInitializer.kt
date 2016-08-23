package org.jetbrains.uast

import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

interface UClassInitializer : UDeclaration, PsiClassInitializer {
    override val psi: PsiClassInitializer
    val uastBody: UExpression

    @Deprecated("Use uastBody instead.", ReplaceWith("uastBody"))
    override fun getBody() = psi.body

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitInitializer(this)) return
        uastAnnotations.acceptList(visitor)
        uastBody.accept(visitor)
        visitor.afterVisitInitializer(this)
    }

    override fun logString() = "UMethod (name = ${psi.name}"
}
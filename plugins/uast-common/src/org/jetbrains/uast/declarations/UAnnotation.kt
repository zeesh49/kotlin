package org.jetbrains.uast

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.psi.PsiElementBacked
import org.jetbrains.uast.visitor.UastVisitor

interface UAnnotation : UElement, PsiAnnotation, PsiElementBacked {
    override val psi: PsiAnnotation
    override fun getOriginalElement() = psi.originalElement

    val languagePlugin: UastLanguagePlugin

    override fun logString() = "UAnnotation"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitAnnotation(this)) return
        visitor.afterVisitAnnotation(this)
    }
}
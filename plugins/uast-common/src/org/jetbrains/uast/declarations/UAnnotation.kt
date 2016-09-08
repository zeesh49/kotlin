package org.jetbrains.uast

import com.intellij.psi.PsiAnnotation
import org.jetbrains.uast.psi.PsiElementBacked
import org.jetbrains.uast.visitor.UastVisitor

/**
 * An annotation wrapper to be used in [UastVisitor].
 */
interface UAnnotation : UElement, PsiAnnotation, PsiElementBacked {
    /**
     * Returns the original annotation (which is *always* unwrapped [PsiAnnotation], never a [UAnnotation]).
     */
    override val psi: PsiAnnotation
    
    override fun getOriginalElement() = psi.originalElement

    /**
     * Returns the plugin for a language in which this annotation is written.
     */
    val languagePlugin: UastLanguagePlugin

    override fun logString() = "UAnnotation"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitAnnotation(this)) return
        visitor.afterVisitAnnotation(this)
    }
}
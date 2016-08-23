package org.jetbrains.uast

import com.intellij.psi.PsiAnnotation

class SimpleUAnnotation(
        psi: PsiAnnotation, 
        override val languagePlugin: UastLanguagePlugin, 
        override val containingElement: UElement?
) : UAnnotation, PsiAnnotation by psi {
    override val psi: PsiAnnotation = unwrap(psi)
    
    private companion object {
        tailrec fun unwrap(psi: PsiAnnotation): PsiAnnotation = if (psi is UAnnotation) unwrap(psi.psi) else psi
    }
}
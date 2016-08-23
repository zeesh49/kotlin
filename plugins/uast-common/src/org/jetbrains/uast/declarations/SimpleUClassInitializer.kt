package org.jetbrains.uast

import com.intellij.psi.PsiClassInitializer
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastLanguagePlugin

class SimpleUClassInitializer(
        psi: PsiClassInitializer, 
        override val languagePlugin: UastLanguagePlugin, 
        override val containingElement: UElement?
) : UClassInitializer, PsiClassInitializer by psi {
    override val psi = unwrap(psi)
    
    override val uastBody by lz { languagePlugin.convertExpressionOrEmpty(psi.body, this) }
    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, languagePlugin, this) } }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()

    private companion object {
        tailrec fun unwrap(psi: PsiClassInitializer): PsiClassInitializer = if (psi is UClassInitializer) unwrap(psi.psi) else psi
    }
}
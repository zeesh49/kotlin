package org.jetbrains.uast

import com.intellij.psi.PsiMethod

open class SimpleUMethod(
        psi: PsiMethod,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : UMethod, PsiMethod by psi {
    override val psi = unwrap(psi)
    
    override val uastBody by lz { languagePlugin.convertOpt<UExpression>(psi.body, this) }
    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, languagePlugin, this) } }
    
    override val uastParameters by lz {
        psi.parameterList.parameters.map { SimpleUParameter(it, languagePlugin, this) } 
    }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()

    private companion object {
        tailrec fun unwrap(psi: PsiMethod): PsiMethod = if (psi is UMethod) unwrap(psi.psi) else psi
    }
}
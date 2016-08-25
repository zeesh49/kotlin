package org.jetbrains.uast

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.psi.PsiElementBacked

interface UDeclaration : UElement, PsiElementBacked, PsiModifierListOwner {
    override val psi: PsiModifierListOwner
    override fun getOriginalElement(): PsiElement? = psi.originalElement
    
    val uastNameIdentifier: UElement?
    
    val languagePlugin: UastLanguagePlugin
    
    val uastAnnotations: List<UAnnotation>

    val isStatic: Boolean
        get() = hasModifierProperty(PsiModifier.STATIC)

    val isFinal: Boolean
        get() = hasModifierProperty(PsiModifier.FINAL)

    val visibility: UastVisibility
        get() = UastVisibility[this]
}
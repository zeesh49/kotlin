package org.jetbrains.uast.kotlin.psi

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.impl.light.LightParameter
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.kotlin.toPsiType

class UastKotlinPsiParameter(
        name: String,
        type: PsiType,
        declarationScope: PsiElement, 
        language: Language, 
        isVarArgs: Boolean
) : LightParameter(name, type, declarationScope, language, isVarArgs) {
    companion object {
        fun create(parameter: KtParameter, owner: PsiElement, index: Int): PsiParameter {
            return UastKotlinPsiParameter(
                    parameter.name ?: "p$index",
                    parameter.typeReference.toPsiType(),
                    owner,
                    KotlinLanguage.INSTANCE,
                    parameter.isVarArg)
        }
    }
}
package org.jetbrains.uast.kotlin.psi

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.impl.light.LightParameter
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.kotlin.analyze
import org.jetbrains.uast.kotlin.toPsiType

class UastKotlinPsiParameter(
        name: String,
        type: PsiType,
        declarationScope: PsiElement,
        language: Language,
        isVarArgs: Boolean,
        val ktDefaultValue: KtExpression?,
        val ktParameter: KtParameter
) : LightParameter(name, type, declarationScope, language, isVarArgs) {
    companion object {
        fun create(parameter: KtParameter, owner: PsiElement, index: Int): PsiParameter {
            return UastKotlinPsiParameter(
                    parameter.name ?: "p$index",
                    (parameter.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, parameter] as? VariableDescriptor)
                            ?.type?.toPsiType(parameter) ?: UastErrorType,
                    owner,
                    KotlinLanguage.INSTANCE,
                    parameter.isVarArg,
                    parameter.defaultValue,
                    parameter)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return ktParameter == (other as? UastKotlinPsiParameter)?.ktParameter
    }

    override fun hashCode() = ktParameter.hashCode()
}
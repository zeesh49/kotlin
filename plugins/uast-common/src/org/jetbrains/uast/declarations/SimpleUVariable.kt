package org.jetbrains.uast

import com.intellij.psi.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.expressions.UTypeReferenceExpression

abstract class AbstractUVariable : PsiVariable, UVariable {
    override val uastInitializer by lz { languagePlugin.convertOpt<UExpression>(psi.initializer, this) }
    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, languagePlugin, this) } }
    override val typeReference by lz { languagePlugin.convertOpt<UTypeReferenceExpression>(psi.typeElement, this) }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()
}

class SimpleUVariable(
        psi: PsiVariable, 
        override val languagePlugin: UastLanguagePlugin, 
        override val containingElement: UElement?
) : AbstractUVariable(), UVariable, PsiVariable by psi {
    override val psi = unwrap(psi)
    
    companion object {
        fun create(psi: PsiVariable, languagePlugin: UastLanguagePlugin, containingElement: UElement?): UVariable {
            return when (psi) {
                is PsiEnumConstant -> SimpleUEnumConstant(psi, languagePlugin, containingElement)
                is PsiLocalVariable -> SimpleULocalVariable(psi, languagePlugin, containingElement)
                is PsiParameter -> SimpleUParameter(psi, languagePlugin, containingElement)
                is PsiField -> SimpleUField(psi, languagePlugin, containingElement)
                else -> SimpleUVariable(psi, languagePlugin, containingElement)
            }
        }
    }
}

class SimpleUParameter(
        psi: PsiParameter,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractUVariable(), UParameter, PsiParameter by psi {
    override val psi = unwrap(psi) as PsiParameter
}

class SimpleUField(
        psi: PsiField,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractUVariable(), UField, PsiField by psi {
    override val psi = unwrap(psi) as PsiField
}

class SimpleULocalVariable(
        psi: PsiLocalVariable,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractUVariable(), ULocalVariable, PsiLocalVariable by psi {
    override val psi = unwrap(psi) as PsiLocalVariable
}

class SimpleUEnumConstant(
        psi: PsiEnumConstant,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractUVariable(), UEnumConstant, PsiEnumConstant by psi {
    override val psi = unwrap(psi) as PsiEnumConstant

    override val isUsedAsExpression: Boolean
        get() = true
    
    override val kind: UastCallKind
        get() = UastCallKind.CONSTRUCTOR_CALL
    override val receiver: UExpression?
        get() = null
    override val receiverType: PsiType?
        get() = null
    override val methodReference: UReferenceExpression?
        get() = null
    override val classReference: UReferenceExpression?
        get() = null
    override val typeArgumentCount: Int
        get() = 0
    override val typeArguments: List<PsiType>
        get() = emptyList()
    override val valueArgumentCount: Int
        get() = psi.argumentList?.expressions?.size ?: 0

    override val valueArguments by lz {
        psi.argumentList?.expressions?.map { languagePlugin.convertExpressionOrEmpty(it, this) } ?: emptyList()
    }

    override val returnType: PsiType?
        get() = psi.type

    override fun resolve() = psi.resolveMethod()

    override val methodName: String?
        get() = null
}

@Suppress("UNCHECKED_CAST")
private tailrec fun <T : PsiVariable> unwrap(psi: T): PsiVariable = if (psi is UVariable) unwrap(psi.psi) else psi
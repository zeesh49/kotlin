package org.jetbrains.uast

import com.intellij.psi.*
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.expressions.UTypeReferenceExpression
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

interface UVariable : UDeclaration, PsiVariable {
    override val psi: PsiVariable
    
    val uastInitializer: UExpression?
    
    val typeReference: UTypeReferenceExpression?

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitVariable(this)) return
        uastAnnotations.acceptList(visitor)
        uastInitializer?.accept(visitor)
        visitor.afterVisitVariable(this)
    }

    @Deprecated("Use uastInitializer instead.", ReplaceWith("uastInitializer"))
    override fun getInitializer() = psi.initializer

    override fun logString() = "UVariable (name = $name)"

    override fun renderString() = buildString {
        val modifiers = PsiModifier.MODIFIERS.filter { psi.hasModifierProperty(it) }.joinToString(" ")
        if (modifiers.isNotEmpty()) append(modifiers).append(' ')
        append("var ").append(psi.name).append(": ").append(psi.type.getCanonicalText(false))
    }
}

interface UParameter : UVariable, PsiParameter {
    override val psi: PsiParameter
}

interface UField : UVariable, PsiField {
    override val psi: PsiField
}

interface ULocalVariable : UVariable, PsiLocalVariable {
    override val psi: PsiLocalVariable
}

interface UEnumConstant : UField, UCallExpression, PsiEnumConstant {
    override val psi: PsiEnumConstant

    override fun logString() = "UEnumConstant (name = ${psi.name}"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitVariable(this)) return
        uastAnnotations.acceptList(visitor)
        methodIdentifier?.accept(visitor)
        classReference?.accept(visitor)
        valueArguments.acceptList(visitor)
        visitor.afterVisitVariable(this)
    }

    override fun renderString() = name ?: "<ERROR>"
}
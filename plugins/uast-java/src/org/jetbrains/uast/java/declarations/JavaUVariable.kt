/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.uast.java

import com.intellij.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.expressions.UTypeReferenceExpression

abstract class AbstractJavaUVariable : PsiVariable, UVariable {
    override val uastInitializer by lz { languagePlugin.convertOpt<UExpression>(psi.initializer, this) }
    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, languagePlugin, this) } }
    override val typeReference by lz { languagePlugin.convertOpt<UTypeReferenceExpression>(psi.typeElement, this) }

    override val uastNameIdentifier: UElement
        get() = UIdentifier(psi.nameIdentifier, this)

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()
}

open class JavaUVariable(
        psi: PsiVariable,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), UVariable, PsiVariable by psi {
    override val psi = unwrap(psi)
    
    companion object {
        fun create(psi: PsiVariable, languagePlugin: UastLanguagePlugin, containingElement: UElement?): UVariable {
            return when (psi) {
                is PsiEnumConstant -> JavaUEnumConstant(psi, languagePlugin, containingElement)
                is PsiLocalVariable -> JavaULocalVariable(psi, languagePlugin, containingElement)
                is PsiParameter -> JavaUParameter(psi, languagePlugin, containingElement)
                is PsiField -> JavaUField(psi, languagePlugin, containingElement)
                else -> JavaUVariable(psi, languagePlugin, containingElement)
            }
        }
    }
}

open class JavaUParameter(
        psi: PsiParameter,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), org.jetbrains.uast.UParameter, PsiParameter by psi {
    override val psi = unwrap(psi) as PsiParameter
}

open class JavaUField(
        psi: PsiField,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), org.jetbrains.uast.UField, PsiField by psi {
    override val psi = unwrap(psi) as PsiField
}

open class JavaULocalVariable(
        psi: PsiLocalVariable,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), org.jetbrains.uast.ULocalVariable, PsiLocalVariable by psi {
    override val psi = unwrap(psi) as PsiLocalVariable
}

open class JavaUEnumConstant(
        psi: PsiEnumConstant,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), org.jetbrains.uast.UEnumConstant, PsiEnumConstant by psi {
    override val psi = unwrap(psi) as PsiEnumConstant

    override val isUsedAsExpression: Boolean
        get() = true
    
    override val kind: org.jetbrains.uast.UastCallKind
        get() = org.jetbrains.uast.UastCallKind.CONSTRUCTOR_CALL
    override val receiver: org.jetbrains.uast.UExpression?
        get() = null
    override val receiverType: PsiType?
        get() = null
    override val methodIdentifier: UIdentifier?
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
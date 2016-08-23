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

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUObjectLiteralExpression(
        override val psi: KtObjectLiteralExpression,
        override val containingElement: UElement?
) : KotlinAbstractUExpression(), UObjectLiteralExpression, PsiElementBacked, KotlinUElementWithType {
    override val declaration by lz { getLanguagePlugin().convert<UClass>(psi.objectDeclaration.toLightClass(), this) }
    override fun getExpressionType() = psi.objectDeclaration.toPsiType()

    //TODO
    override val methodReference: UReferenceExpression?
        get() = null
    override val classReference: UReferenceExpression?
        get() = null
    override val valueArgumentCount: Int
        get() = 0
    override val valueArguments: List<UExpression>
        get() = emptyList()
    override val typeArgumentCount: Int
        get() = 0
    override val typeArguments: List<PsiType>
        get() = emptyList()

    override fun resolve() = null
}
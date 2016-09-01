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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

open class KotlinUSimpleReferenceExpression(
        override val psi: PsiElement,
        override val identifier: String,
        override val containingElement: UElement?,
        private val resultingDescriptor: DeclarationDescriptor? = null
) : KotlinAbstractUExpression(), USimpleNameReferenceExpression, PsiElementBacked, KotlinUElementWithType, KotlinEvaluatableUElement {
    override fun resolve() = (psi as? KtElement)?.resolveCallToDeclaration(this, resultingDescriptor)
    override val resolvedName: String?
        get() = (resolve() as? PsiNamedElement)?.name
}

class KotlinNameUSimpleReferenceExpression(
        psi: PsiElement,
        identifier: String,
        parent: UElement?,
        descriptor: DeclarationDescriptor? = null
) : KotlinUSimpleReferenceExpression(psi, identifier, parent, descriptor) {
    
}

class KotlinClassViaConstructorUSimpleReferenceExpression(
        override val psi: KtCallExpression,
        override val identifier: String,
        override val containingElement: UElement?
) : KotlinAbstractUExpression(), USimpleNameReferenceExpression, PsiElementBacked, KotlinUElementWithType {
    override val resolvedName: String?
        get() = (psi.getResolvedCall(psi.analyze())?.resultingDescriptor as? ConstructorDescriptor)
                ?.containingDeclaration?.name?.asString()

    override fun resolve(): PsiElement? {
        val resolvedCall = psi.getResolvedCall(psi.analyze())
        val resultingDescriptor = resolvedCall?.resultingDescriptor as? ConstructorDescriptor ?: return null
        val clazz = resultingDescriptor.containingDeclaration
        return clazz.toSource()?.getMaybeLightElement(this)
    }
}

class KotlinStringUSimpleReferenceExpression(
        override val identifier: String,
        override val containingElement: UElement?
) : KotlinAbstractUExpression(), USimpleNameReferenceExpression {
    override fun resolve() = null
    override val resolvedName: String?
        get() = identifier
}
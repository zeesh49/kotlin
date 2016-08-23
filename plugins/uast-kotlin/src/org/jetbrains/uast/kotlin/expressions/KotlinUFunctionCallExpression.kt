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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUFunctionCallExpression(
        override val psi: KtCallExpression,
        override val containingElement: UElement?,
        private val _resolvedCall: ResolvedCall<*>? = null
) : KotlinAbstractUExpression(), UCallExpression, PsiElementBacked, KotlinUElementWithType {
    private val resolvedCall by lz {
        _resolvedCall ?: psi.getResolvedCall(psi.analyze()) 
    }
    
    override val receiverType by lz {
        val resolvedCall = this.resolvedCall ?: return@lz null
        val receiver = resolvedCall.extensionReceiver ?: resolvedCall.dispatchReceiver ?: return@lz null
        receiver.type.toPsiType(psi)
    }

    override val methodName by lz { resolvedCall?.resultingDescriptor?.name?.asString() }

    override val classReference by lz {
        KotlinClassViaConstructorUSimpleReferenceExpression(psi, methodName.orAnonymous("class"), this)
    }

    override val methodReference by lz {
        val calleeExpression = psi.calleeExpression ?: return@lz null
        val name = (calleeExpression as? KtSimpleNameExpression)?.getReferencedName() ?: return@lz null
        KotlinNameUSimpleReferenceExpression(calleeExpression, name, this)
    }

    override val valueArgumentCount: Int
        get() = psi.valueArguments.size

    override val valueArguments by lz { psi.valueArguments.map { KotlinConverter.convertOrEmpty(it.getArgumentExpression(), this) } }

    override val typeArgumentCount: Int
        get() = psi.typeArguments.size

    override val typeArguments by lz { psi.typeArguments.map { it.typeReference.toPsiType() } }

    override val returnType: PsiType?
        get() = getExpressionType()

    override val kind by lz {
        when (resolvedCall?.resultingDescriptor) {
            is ConstructorDescriptor -> UastCallKind.CONSTRUCTOR_CALL
            else -> UastCallKind.METHOD_CALL
        }
    }

    override val receiver: UExpression?
        get() = throw UnsupportedOperationException()

    override fun resolve(): PsiMethod? {
        val descriptor = resolvedCall?.resultingDescriptor ?: return null
        val source = descriptor.toSource() ?: return null

        if (descriptor is ConstructorDescriptor && descriptor.isPrimary
                && source is KtClassOrObject && source.getPrimaryConstructor() == null
                && source.getSecondaryConstructors().isEmpty()) {
            return source.toLightClass()?.constructors?.firstOrNull()
        }
        
        return when (source) {
            is KtFunction -> LightClassUtil.getLightClassMethod(source)
            is PsiMethod -> source
            else -> null
        }
    }
}

class KotlinUComponentFunctionCallExpression(
        override val psi: PsiElement,
        val n: Int,
        override val containingElement: UElement?
) : UCallExpression, PsiElementBacked {
    override val receiverType: PsiType?
        get() = null
    
    override val valueArgumentCount: Int
        get() = 0
    
    override val valueArguments: List<UExpression>
        get() = emptyList()
    
    override val typeArgumentCount: Int
        get() = 0
    
    override val methodName: String
        get() = "component$n"

    override val returnType: PsiType?
        get() = getExpressionType()
    
    override val methodReference by lz { KotlinStringUSimpleReferenceExpression(methodName, this) }
    
    override val kind: UastCallKind
        get() = UastCallKind.METHOD_CALL

    override val classReference: UReferenceExpression?
        get() = null
    
    override val typeArguments: List<PsiType>
        get() = emptyList()

    override val receiver: UExpression?
        get() = throw UnsupportedOperationException()

    override val isUsedAsExpression: Boolean
        get() = true

    override fun resolve() = null
}
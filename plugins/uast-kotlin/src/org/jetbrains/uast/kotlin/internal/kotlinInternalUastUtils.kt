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

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable
import java.text.StringCharacterIterator

@Suppress("NOTHING_TO_INLINE")
internal inline fun String?.orAnonymous(kind: String = ""): String {
    return this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"
}

internal tailrec fun UElement.getLanguagePlugin(): UastLanguagePlugin {
    return if (this is UDeclaration) languagePlugin else containingElement!!.getLanguagePlugin()
}

internal fun DeclarationDescriptor.toSource() = try {
    DescriptorToSourceUtils.descriptorToDeclaration(this)
} catch (e: Exception) {
    null
}

internal fun <T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

internal fun KotlinType.toPsiType(element: KtElement): PsiType {
    if (this.isError) return UastErrorType
    
    val project = element.project
    val typeMapper = ServiceManager.getService(project, KotlinUastBindingContextProviderService::class.java)
            .getTypeMapper(element) ?: return UastErrorType
    
    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
    typeMapper.mapType(this, signatureWriter, TypeMappingMode.DEFAULT)
    
    val signature = StringCharacterIterator(signatureWriter.toString())
    
    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return UastErrorType
    return ClsTypeElementImpl(element, typeText, '\u0000').type
}

internal fun KtTypeReference?.toPsiType(): PsiType {
    if (this == null) return UastErrorType
    return (analyze()[BindingContext.TYPE, this] ?: return UastErrorType).toPsiType(this)
}

internal fun KtClassOrObject.toPsiType(): PsiType {
    val lightClass = toLightClass() ?: return UastErrorType
    return PsiTypesUtil.getClassType(lightClass)
}

internal fun PsiElement.getMaybeLightElement(context: UElement): PsiElement? {
    return when (this) {
        is KtVariableDeclaration -> {
            val lightElement = toLightElements().firstOrNull()
            if (lightElement != null) return lightElement
            
            val languagePlugin = context.getLanguagePlugin()
            val uElement = languagePlugin.convertWithParent(this)
            when (uElement) {
                is UDeclaration -> uElement.psi
                is UVariableDeclarationsExpression -> uElement.variables.firstOrNull()?.psi
                else -> null
            }
        }
        is KtDeclaration -> toLightElements().firstOrNull()
        is KtElement -> null
        else -> this
    }
}

internal fun KtElement.resolveCallToDeclaration(
        resultingDescriptor: DeclarationDescriptor? = null,
        context: KotlinAbstractUElement
): PsiElement? {
    val descriptor = resultingDescriptor ?: run {
        val resolvedCall = getResolvedCall(analyze()) ?: return null
        resolvedCall.resultingDescriptor
    }
    
    return descriptor.toSource()?.getMaybeLightElement(context)
}

internal fun KtExpression?.isNullExpression(): Boolean {
    return this?.unwrapBlockOrParenthesis()?.node?.elementType == KtNodeTypes.NULL
}

internal fun KtExpression.unwrapBlockOrParenthesis(): KtExpression {
    val innerExpression = KtPsiUtil.safeDeparenthesize(this)
    if (innerExpression is KtBlockExpression) {
        val statement = innerExpression.statements.singleOrNull() ?: return this
        return KtPsiUtil.safeDeparenthesize(statement)
    }
    return innerExpression
}

internal fun KtElement.analyze(): BindingContext {
    return ServiceManager.getService(project, KotlinUastBindingContextProviderService::class.java)
            ?.getBindingContext(this) ?: BindingContext.EMPTY
}
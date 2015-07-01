/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ModifiersChecker
import org.jetbrains.kotlin.resolve.TraceBasedRedeclarationHandler
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.lazy.data.KotlinFunctionLikeInfo
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl
import org.jetbrains.kotlin.types.TypeSubstitutor

//public class LazyFunctionDescriptor(
//        val lazyContext: LazyClassContext,
//        val functionInfo: KotlinFunctionLikeInfo,
//        val _containingDescriptor: DeclarationDescriptor,
//        val _original: FunctionDescriptor?,
//        val _name: Name,
//        val _kind: CallableMemberDescriptor.Kind,
//        val _source: SourceElement) : FunctionDescriptor, LazyEntity {
//
//    private val _visibility: Visibility
//    private val _modality: Modality
//    private val _annotations: Annotations
//
//    private val innerScope = lazyContext.storageManager.createLazyValue {
//        WritableScopeImpl(lazyContext.declarationScopeProvider.getResolutionScopeForDeclaration(functionInfo.getCorrespondingFunction()),
//                          this, TraceBasedRedeclarationHandler(lazyContext.trace), "Function descriptor header scope") }
//
//    private val _returnType = lazyContext.storageManager.createNullableLazyValue {
//        functionInfo.getCorrespondingFunction().getTypeReference()?.let {
//            lazyContext.typeResolver.resolveType(innerScope.invoke(), it, lazyContext.trace, true)
//        }
//    }
//
//    private val _parameters = lazyContext.storageManager.createLazyValue {
//        val typeParameterList = functionInfo.getCorrespondingFunction().getTypeParameterList()
//        if (typeParameterList == null)
//            listOf()
//        else {
//            val typeParameters = typeParameterList.getParameters();
//            typeParameters.mapIndexed { i, parameter -> LazyTypeParameterDescriptor(lazyContext, this, typeParameters.get(i), i) }
//        }
//    }
//
//    private val _extensionReceiverParameter = lazyContext.storageManager.createNullableLazyValue {
//        DescriptorUtils.getDispatchReceiverParameterIfNeeded(_containingDescriptor)
//    }
//
//    init {
//        val function = functionInfo.getCorrespondingFunction()
//
//        _visibility = ModifiersChecker.resolveVisibilityFromModifiers(function, DescriptorResolver.getDefaultVisibility(function, _containingDescriptor))
//        _modality = ModifiersChecker.resolveModalityFromModifiers(function, DescriptorResolver.getDefaultModality(_containingDescriptor, _visibility, function.hasBody()))
//
//        val jetModifierList = function.getModifierList()
//        _annotations = if (jetModifierList != null) {
//            LazyAnnotations(object : LazyAnnotationsContext(lazyContext.annotationResolver, lazyContext.storageManager, lazyContext.trace) {
//                override val scope: JetScope = lazyContext.declarationScopeProvider.getResolutionScopeForDeclaration(function)
//            }, jetModifierList.getAnnotationEntries())
//        }
//        else {
//            Annotations.EMPTY
//        }
//    }
//
//    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
//        throw UnsupportedOperationException()
//    }
//
//    override fun getOverriddenDescriptors(): MutableSet<out FunctionDescriptor> {
//        throw UnsupportedOperationException()
//    }
//
//    override fun copy(newOwner: DeclarationDescriptor?, modality: Modality?, visibility: Visibility?, kind: CallableMemberDescriptor.Kind?, copyOverrides: Boolean): FunctionDescriptor {
//        throw UnsupportedOperationException()
//    }
//
//    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
//        throw UnsupportedOperationException()
//    }
//
//    override fun getValueParameters(): List<ValueParameterDescriptor> {
//        throw UnsupportedOperationException()
//    }
//
//    override fun addOverriddenDescriptor(overridden: CallableMemberDescriptor) {
//        throw UnsupportedOperationException()
//    }
//
//    override fun forceResolveAllContents() {
//        throw UnsupportedOperationException()
//    }
//
//    override fun getContainingDeclaration() = _containingDescriptor
//    override fun getName() = _name
//    override fun getKind() = _kind
//    override fun getSource() = _source
//
//    override fun getModality() = _modality
//    override fun getVisibility(): Visibility = _visibility
//    override fun getOriginal(): FunctionDescriptor = _original ?: this
//
//    override fun getReturnType() = _returnType.invoke()
//    override fun getTypeParameters() = _parameters.invoke()
//    override fun getExtensionReceiverParameter() = _extensionReceiverParameter.invoke()
//
//    override fun getAnnotations(): Annotations = _annotations
//
//    override fun hasStableParameterNames() = true
//    override fun hasSynthesizedParameterNames() = false
//
//    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R = visitor.visitFunctionDescriptor(this, data)
//    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
//        visitor?.visitFunctionDescriptor(this, null)
//    }
//}

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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias

import com.intellij.util.containers.LinkedMultiMap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class IntroduceTypeAliasParameterEditor(originalDescriptor: IntroduceTypeAliasDescriptor) {
    companion object {
        val MAX_HISTORY_SIZE = 16
    }

    private val descriptors = arrayListOf(originalDescriptor)
    private var descriptorIndex = 0

    var currentDescriptor: IntroduceTypeAliasDescriptor
        get() = descriptors[descriptorIndex]
        set(value) {
            descriptors[descriptorIndex] = value
        }

    val canUndo: Boolean
        get() = descriptorIndex > 0

    val canRedo: Boolean
        get() = descriptorIndex < descriptors.lastIndex

    private fun addDescriptor(descriptor: IntroduceTypeAliasDescriptor) {
        descriptors.dropLast(descriptors.size - descriptorIndex - 1)
        if (descriptors.size == MAX_HISTORY_SIZE) {
            descriptors.removeAt(0)
        }
        descriptors += descriptor
        descriptorIndex = descriptors.lastIndex
    }

    private fun KtTypeElement.index(): Int {
        val parent = parent
        if (parent is KtNullableType) return 0
        val typeRef = parent as? KtTypeReference ?: return -1
        val containingType = typeRef.getStrictParentOfType<KtTypeElement>() ?: return -1
        return containingType.typeArgumentsAsTypes.indexOf(typeRef)
    }

    private fun KtTypeElement.childType(index: Int): KtTypeElement? {
        if (this is KtNullableType && index == 0) return innerType
        return typeArgumentsAsTypes.getOrNull(index)?.typeElement
    }

    private fun KtTypeElement.findOriginal(typeAlias: KtTypeAlias): KtTypeElement? {
        val rootTypeElement = typeAlias.getTypeReference()!!.typeElement!!
        val indices = parentsWithSelf
                .filterIsInstance<KtTypeElement>()
                .takeWhile { it != rootTypeElement }
                .map { it.index() }
                .toList()
                .asReversed()
        if (indices.isEmpty()) return rootTypeElement
        return indices.fold(currentDescriptor.originalData.originalType as KtTypeElement?) {
            typeElement, index -> typeElement?.childType(index)
        }
    }

    fun foldSingle(name: String, typeReference: KtTypeReference, typeAlias: KtTypeAlias): Boolean {
        val typeElement = typeReference.typeElement ?: return false
        if (!typeAlias.isAncestor(typeReference)) throw IllegalArgumentException("Not a generated type alias element")

        val originalTypeElement = typeElement.findOriginal(typeAlias) ?: return false
        val originalTypeReference = originalTypeElement.parent as? KtTypeReference ?: return false

        val referencesToRemove = LinkedMultiMap<TypeParameter, TypeReferenceInfo>()
        originalTypeReference.forEachDescendantOfType<KtTypeReference> {
            val typeReferenceInfo = it.resolveInfo ?: return@forEachDescendantOfType
            val typeParameter = currentDescriptor.typeParameters.firstOrNull { typeReferenceInfo in it.typeReferenceInfos } ?: return@forEachDescendantOfType
            referencesToRemove.putValue(typeParameter, typeReferenceInfo)
        }

        val typeParameters = currentDescriptor.typeParameters.toMutableList()

        for ((typeParameter, typeReferenceInfos) in referencesToRemove.entrySet()) {
            val newTypeReferenceInfos = typeParameter.typeReferenceInfos - typeReferenceInfos
            if (newTypeReferenceInfos.isEmpty()) {
                typeParameters -= typeParameter
            }
            else {
                typeParameters[typeParameters.indexOf(typeParameter)] = typeParameter.copy(typeReferenceInfos = newTypeReferenceInfos)
            }
        }

        val type = originalTypeElement.getAbbreviatedTypeOrType(currentDescriptor.originalData.bindingContext)
                   ?: currentDescriptor.originalData.resolutionFacade.moduleDescriptor.builtIns.nullableAnyType

        typeParameters += TypeParameter(name, listOf(TypeReferenceInfo(originalTypeReference, type)))

        addDescriptor(currentDescriptor.copy(typeParameters = typeParameters))

        return true
    }

    fun undo() {
        if (!canUndo) throw IllegalStateException("Can't undo")
        descriptorIndex--
    }

    fun redo() {
        if (!canRedo) throw IllegalStateException("Can't redo")
        descriptorIndex++
    }
}
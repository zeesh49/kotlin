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

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.js.PredefinedAnnotation
import org.jetbrains.kotlin.psi.JetBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.JetIsExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TypeInfoOperationChecker
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.hasAnnotation

class NativeTypeInfoOperationChecker : TypeInfoOperationChecker {
    override fun checkIsExpression(expression: JetIsExpression, trace: BindingTrace) {
        val typeReference = expression.getTypeReference()
        val type = BindingContextUtils.getNotNull(trace.getBindingContext(), BindingContext.TYPE, typeReference);
        val descriptor = type.getConstructor().getDeclarationDescriptor() ?: return

        if (!hasAnnotation(descriptor, PredefinedAnnotation.NATIVE)) return

        
    }

    override fun checkBinaryExpressionWithTypeRHS(expression: JetBinaryExpressionWithTypeRHS, trace: BindingTrace) {

    }
}

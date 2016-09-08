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

package org.jetbrains.uast.kotlin.declarations

import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.*
import org.jetbrains.uast.java.JavaUMethod
import org.jetbrains.uast.kotlin.lz

open class KotlinUMethod(
        psi: KtLightMethod,
        languagePlugin: UastLanguagePlugin,
        containingElement: UElement?
) : JavaUMethod(psi, languagePlugin, containingElement) {
    override val psi: KtLightMethod = psi
    private val kotlinOrigin = (psi.originalElement as KtLightElement<*, *>).kotlinOrigin

    override val uastBody by lz {
        val originalElement = psi.originalElement
        val bodyExpression = when (originalElement) {
            is KtLightMethod -> (kotlinOrigin as? KtFunction)?.bodyExpression
            else -> null
        } ?: return@lz null
        languagePlugin.convertOpt<UExpression>(bodyExpression, this)
    }
    
    companion object {
        fun create(psi: KtLightMethod, languagePlugin: UastLanguagePlugin, containingElement: UElement?) = when (psi) {
            is KtLightMethodImpl.KtLightAnnotationMethod -> KotlinUAnnotationMethod(psi, languagePlugin, containingElement)
            else -> KotlinUMethod(psi, languagePlugin, containingElement)
        }
    }
}

class KotlinUAnnotationMethod(
        override val psi: KtLightMethodImpl.KtLightAnnotationMethod,
        languagePlugin: UastLanguagePlugin,
        containingElement: UElement?
) : KotlinUMethod(psi, languagePlugin, containingElement), UAnnotationMethod {
    override val uastDefaultValue by lz {
        val annotationParameter = psi.kotlinOrigin as? KtParameter ?: return@lz null
        val defaultValue = annotationParameter.defaultValue ?: return@lz null
        languagePlugin.convertOpt<UExpression>(defaultValue, this)
    }
}
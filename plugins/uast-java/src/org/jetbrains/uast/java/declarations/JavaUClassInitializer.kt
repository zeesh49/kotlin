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

import com.intellij.psi.PsiClassInitializer
import org.jetbrains.uast.*

class JavaUClassInitializer(
        psi: PsiClassInitializer, 
        override val languagePlugin: UastLanguagePlugin, 
        override val containingElement: UElement?
) : UClassInitializer, PsiClassInitializer by psi {
    override val psi = unwrap(psi)

    override val uastNameIdentifier: UElement?
        get() = null
    
    override val uastBody by lz { languagePlugin.convertOpt(psi.body, this) ?: UastEmptyExpression }
    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, languagePlugin, this) } }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()

    private companion object {
        tailrec fun unwrap(psi: PsiClassInitializer): PsiClassInitializer = if (psi is UClassInitializer) unwrap(psi.psi) else psi
    }
}
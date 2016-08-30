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

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.*
import org.jetbrains.uast.java.AbstractJavaUClass
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod

class KotlinUClass private constructor(
        psi: KtLightClass,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractJavaUClass(), PsiClass by psi {
    val ktClass = psi.kotlinOrigin
    override val psi = unwrap(psi)

    override val uastNameIdentifier: UElement
        get() = UIdentifier(psi.nameIdentifier, this)
    
    override val uastMethods: List<UMethod> by lz {
        val primaryConstructor = ktClass?.getPrimaryConstructor()?.toLightMethods()?.firstOrNull()
        val hasSecondaryConstructors = ktClass?.getSecondaryConstructors()?.isNotEmpty() ?: false
        psi.methods.map {
            if (it is KtLightMethod && it.isConstructor 
                    && (it == primaryConstructor || (primaryConstructor == null && !hasSecondaryConstructors))) {
                object : KotlinUMethod(it, languagePlugin, this@KotlinUClass) {
                    override val uastBody by lz {
                        val initializers = ktClass?.getAnonymousInitializers() ?: return@lz UastEmptyExpression
                        val containingMethod = this
                        
                        object : UBlockExpression {
                            override val containingElement: UElement?
                                get() = containingMethod
                            
                            override val expressions by lz {
                                initializers.map { languagePlugin.convertExpressionOrEmpty(it.body, this) } 
                            }
                            
                            override val isUsedAsExpression: Boolean
                                get() = false
                        }
                    }
                }
            } 
            else {
                languagePlugin.convert<UMethod>(it, this)
            }
        } 
    }

    companion object {
        private tailrec fun unwrap(psi: PsiClass): PsiClass = if (psi is UClass) unwrap(psi.psi) else psi

        fun create(psi: KtLightClass, languagePlugin: UastLanguagePlugin, containingElement: UElement?): UClass {
            return if (psi is PsiAnonymousClass)
                KotlinUAnonymousClass(psi, languagePlugin, containingElement)
            else
                KotlinUClass(psi, languagePlugin, containingElement)
        }
    }
}

class KotlinUAnonymousClass(
        psi: PsiAnonymousClass,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractJavaUClass(), UAnonymousClass, PsiAnonymousClass by psi {
    override val psi: PsiAnonymousClass = unwrap(psi)

    override val uastNameIdentifier: UElement?
        get() {
            val ktClassOrObject = (psi.originalElement as? KtLightClass)?.kotlinOrigin as? KtObjectDeclaration ?: return null 
            return UIdentifier(ktClassOrObject.getObjectKeyword(), this)
        }

    private companion object {
        tailrec fun unwrap(psi: PsiAnonymousClass): PsiAnonymousClass = if (psi is UAnonymousClass) unwrap(psi.psi) else psi
    }
}
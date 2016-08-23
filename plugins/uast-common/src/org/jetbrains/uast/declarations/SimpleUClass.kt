package org.jetbrains.uast

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

abstract class AbstractUClass : UClass {
    override val uastDeclarations by lz {
        mutableListOf<UDeclaration>().apply {
            addAll(uastFields)
            addAll(uastInitializers)
            addAll(uastMethods)
            addAll(uastNestedClasses)
        }
    }

    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, languagePlugin, this) } }
    
    override val uastFields: List<UVariable> by lz { psi.fields.map { languagePlugin.convert<UVariable>(it, this) } }
    override val uastInitializers: List<UClassInitializer> by lz { psi.initializers.map { languagePlugin.convert<UClassInitializer>(it, this) } }
    override val uastMethods: List<UMethod> by lz { psi.methods.map { languagePlugin.convert<UMethod>(it, this) } }
    override val uastNestedClasses: List<UClass> by lz { psi.innerClasses.map { languagePlugin.convert<UClass>(it, this) } }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()
}

class SimpleUClass private constructor(
        psi: PsiClass, 
        override val languagePlugin: UastLanguagePlugin, 
        override val containingElement: UElement?
) : AbstractUClass(), PsiClass by psi {
    override val psi = unwrap(psi)
    
    companion object {
        private tailrec fun unwrap(psi: PsiClass): PsiClass = if (psi is UClass) unwrap(psi.psi) else psi
        
        fun create(psi: PsiClass, languagePlugin: UastLanguagePlugin, containingElement: UElement?): UClass {
            return if (psi is PsiAnonymousClass) 
                SimpleUAnonymousClass(psi, languagePlugin, containingElement)
            else
                SimpleUClass(psi, languagePlugin, containingElement)
        }
    }
}

class SimpleUAnonymousClass(
        psi: PsiAnonymousClass,
        override val languagePlugin: UastLanguagePlugin,
        override val containingElement: UElement?
) : AbstractUClass(), UAnonymousClass, PsiAnonymousClass by psi {
    override val psi: PsiAnonymousClass = unwrap(psi)

    private companion object {
        tailrec fun unwrap(psi: PsiAnonymousClass): PsiAnonymousClass = if (psi is UAnonymousClass) unwrap(psi.psi) else psi
    }
}
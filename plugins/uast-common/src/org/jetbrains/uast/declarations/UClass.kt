package org.jetbrains.uast

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.SimpleUVariable
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

interface UClass : UDeclaration, PsiClass {
    override val psi: PsiClass
    
    fun getUastSuperClass(): UClass? {
        val superClass = superClass ?: return null
        return languagePlugin.context.convertWithParent(superClass) as? UClass
    }
    
    val uastDeclarations: List<UDeclaration>
    
    val uastFields: List<UVariable>
    val uastInitializers: List<UClassInitializer>
    val uastMethods: List<UMethod>
    val uastNestedClasses: List<UClass>

    override fun logString() = "UClass (name = $name)"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitClass(this)) return
        uastAnnotations.acceptList(visitor)
        uastDeclarations.acceptList(visitor)
        visitor.afterVisitClass(this)
    }
}

interface UAnonymousClass : UClass, PsiAnonymousClass {
    override val psi: PsiAnonymousClass
}
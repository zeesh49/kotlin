package org.jetbrains.uast

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

interface UFile : UElement {
    val psi: PsiFile
    
    val packageName: String
    val imports: List<UImportStatement>
    val classes: List<UClass>
    
    val languagePlugin: UastLanguagePlugin

    override fun logString() = "UFile"
    
    fun getIoFile(): File? = psi.virtualFile?.let { VfsUtilCore.virtualToIoFile(it) }

    override val containingElement: UElement?
        get() = null

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitFile(this)) return
        imports.acceptList(visitor)
        classes.acceptList(visitor)
        visitor.afterVisitFile(this)
    }
}


package org.jetbrains.uast

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFile
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

/**
 * Represents a Uast file.
 */
interface UFile : UElement {
    /**
     * Returns the original [PsiFile].
     */
    val psi: PsiFile

    /**
     * Returns the Java package name of this file.
     * Returns an empty [String] for the default package. 
     */
    val packageName: String

    /**
     * Returns the import statements for this file.
     */
    val imports: List<UImportStatement>

    /**
     * Returns the list of top-level classes declared in this file.
     */
    val classes: List<UClass>

    /**
     * Returns the plugin for a language used in this file.
     */
    val languagePlugin: UastLanguagePlugin

    override fun logString() = "UFile"

    /**
     * Get a physical [File] for this file, or null if there is no such file on disk.
     */
    fun getIoFile(): File? = psi.virtualFile?.let { VfsUtilCore.virtualToIoFile(it) }

    /**
     * [UFile] is a top-level element of the Uast hierarchy, thus the [containingElement] always returns null for it.
     */
    override val containingElement: UElement?
        get() = null

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitFile(this)) return
        imports.acceptList(visitor)
        classes.acceptList(visitor)
        visitor.afterVisitFile(this)
    }
}


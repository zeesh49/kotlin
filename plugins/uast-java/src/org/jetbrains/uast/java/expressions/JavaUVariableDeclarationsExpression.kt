package org.jetbrains.uast.java

import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UVariableDeclarationsExpression

class JavaUVariableDeclarationsExpression(
        override val containingElement: UElement?
) : UVariableDeclarationsExpression {
    override lateinit var variables: List<UVariable>
        internal set

    override val isUsedAsExpression: Boolean
        get() = false

    constructor(parent: UElement?, variables: List<UVariable>) : this(parent) {
        this.variables = variables
    }
}
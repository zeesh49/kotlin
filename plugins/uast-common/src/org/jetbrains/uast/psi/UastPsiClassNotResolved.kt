package org.jetbrains.uast.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightPsiClassBuilder

class UastPsiClassNotResolved(context: PsiElement) : LightPsiClassBuilder(context, "Error")
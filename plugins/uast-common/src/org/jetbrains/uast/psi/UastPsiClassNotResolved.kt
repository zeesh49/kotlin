package org.jetbrains.uast.psi

import com.intellij.lang.Language
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.light.AbstractLightClass
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightPsiClassBuilder

class UastPsiClassNotResolved(context: PsiElement) : LightPsiClassBuilder(context, "Error")
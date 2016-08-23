/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.java.expressions.JavaUSynchronizedExpression

class JavaUastLanguagePlugin : UastLanguagePlugin() {
    override fun getMethodCallExpression(
            e: PsiElement, 
            containingClassFqName: String?, 
            methodName: String
    ): Pair<UCallExpression, PsiMethod>? {
        if (e !is PsiMethodCallExpression) return null
        if (e.methodExpression.referenceName != methodName) return null
        
        val uElement = convertWithParent(e)
        val callExpression = when (uElement) {
            is UCallExpression -> uElement
            is UQualifiedReferenceExpression -> uElement.selector as UCallExpression
            else -> error("Invalid element type: $uElement")
        }
        
        val method = callExpression.resolve() ?: return null
        if (containingClassFqName != null) {
            val containingClass = method.containingClass ?: return null
            if (containingClass.qualifiedName != containingClassFqName) return null
        }
        
        return Pair(callExpression, method)
    }
    
    override fun getConstructorCallExpression(
            e: PsiElement, 
            fqName: String
    ): Triple<UCallExpression, PsiMethod, PsiClass>? {
        if (e !is PsiNewExpression) return null
        val simpleName = fqName.substringAfterLast('.')
        if (e.classReference?.referenceName != simpleName) return null
        
        val callExpression = convertWithParent(e) as? UCallExpression ?: return null
        
        val constructorMethod = e.resolveConstructor() ?: return null
        val containingClass = constructorMethod.containingClass ?: return null
        if (containingClass.qualifiedName != fqName) return null
        
        return Triple(callExpression, constructorMethod, containingClass)
    }

    override val priority = 0

    override fun isFileSupported(fileName: String): Boolean {
        return fileName.endsWith(".java", ignoreCase = true)
    }

    override fun convertElement(element: Any?, parent: UElement?): UElement? {
        if (element !is PsiElement) return null
        return convertDeclaration(element, parent) ?: JavaConverter.convertPsiElement(element, parent)
    }

    override fun convertWithParent(element: Any?): UElement? {
        if (element !is PsiElement) return null
        if (element is PsiJavaFile) return JavaUFile(element, this)
        JavaConverter.getCached<UElement>(element)?.let { return it }

        val parent = JavaConverter.unwrapParents(element.parent) ?: return null
        val parentUElement = convertWithParent(parent) ?: return null
        return convert(element, parentUElement)
    }
    
    private fun convertDeclaration(element: PsiElement, parent: UElement?): UElement? {
        return when (element) {
            is PsiJavaFile -> JavaUFile(element, this)
            is UDeclaration -> element
            is PsiClass -> SimpleUClass.create(element, this, parent)
            is PsiMethod -> SimpleUMethod(element, this, parent)
            is PsiVariable -> SimpleUVariable.create(element, this, parent)
            is UAnnotation -> SimpleUAnnotation(element, this, parent)
            else -> null
        }
    }
}

internal object JavaConverter : UastConverter {
//    override fun convertWithoutParent(element: Any?): UElement? {
//        if (element !is PsiElement) return null
//        return convertPsiElement(element, null)
//    }

    internal inline fun <reified T : UElement> getCached(element: PsiElement): T? {
        return null
//        if (!element.isValid) return null
//        return element.getUserData(CACHED_UELEMENT_KEY)?.get() as? T
    }

    internal tailrec fun unwrapParents(parent: PsiElement?): PsiElement? = when (parent) {
        is PsiExpressionStatement -> unwrapParents(parent.parent)
        is PsiParameterList -> unwrapParents(parent.parent)
        is PsiAnnotationParameterList -> unwrapParents(parent.parent)
        else -> parent
    }

    internal fun convertPsiElement(el: PsiElement, parent: UElement?): UElement? {
        getCached<UElement>(el)?.let { return it }

        return when (el) {
            is PsiCodeBlock -> convertBlock(el, parent)
            is PsiResourceExpression -> convertExpression(el.expression, parent)
            is PsiExpression -> convertExpression(el, parent)
            is PsiStatement -> convertStatement(el, parent)
            is PsiIdentifier -> JavaUSimpleNameReferenceExpression(el, el.text, parent)
            is PsiNameValuePair -> convertNameValue(el, parent)
            is PsiArrayInitializerMemberValue -> JavaAnnotationArrayInitializerUCallExpression(el, parent)
            else -> null
        }
    }
    
    internal fun convertBlock(block: PsiCodeBlock, parent: UElement?): UBlockExpression =
        getCached(block) ?: JavaUCodeBlockExpression(block, parent)

    internal fun convertNameValue(pair: PsiNameValuePair, parent: UElement?): UNamedExpression {
        return UNamedExpression.create(pair.name.orAnonymous(), parent) {
            val value = pair.value as? PsiElement
            value?.let { convertPsiElement(it, this) as? UExpression } ?: UnknownJavaExpression(value ?: pair, this)
        }
    }

    internal fun convertReference(expression: PsiReferenceExpression, parent: UElement?): UExpression {
        return if (expression.isQualified) {
            JavaUQualifiedExpression(expression, parent)
        } else {
            val name = expression.referenceName ?: "<error name>"
            JavaUSimpleNameReferenceExpression(expression, name, parent, expression)
        }
    }

    private fun convertPolyadicExpression(
        expression: PsiPolyadicExpression,
        parent: UElement?,
        i: Int
    ): UBinaryExpression {
        return if (i == 1) JavaSeparatedPolyadicUBinaryExpression(expression, parent).apply {
            leftOperand = convertExpression(expression.operands[0], this)
            rightOperand = convertExpression(expression.operands[1], this)
        } else JavaSeparatedPolyadicUBinaryExpression(expression, parent).apply {
            leftOperand = convertPolyadicExpression(expression, parent, i - 1)
            rightOperand = convertExpression(expression.operands[i], this)
        }
    }
    
    internal fun convertExpression(el: PsiExpression, parent: UElement?): UExpression {
        getCached<UExpression>(el)?.let { return it }

        return when (el) {
            is PsiPolyadicExpression -> convertPolyadicExpression(el, parent, el.operands.size - 1)
            is PsiAssignmentExpression -> JavaUAssignmentExpression(el, parent)
            is PsiConditionalExpression -> JavaUTernaryIfExpression(el, parent)
            is PsiNewExpression -> {
                if (el.anonymousClass != null)
                    JavaUObjectLiteralExpression(el, parent)
                else
                    JavaConstructorUCallExpression(el, parent)
            }
            is PsiMethodCallExpression -> {
                if (el.methodExpression.qualifierExpression != null)
                    JavaUCompositeQualifiedExpression(el, parent).apply {
                        receiver = convertExpression(el.methodExpression.qualifierExpression!!, this)
                        selector = JavaUCallExpression(el, this)
                    }
                else
                    JavaUCallExpression(el, parent)
            }
            is PsiArrayInitializerExpression -> JavaArrayInitializerUCallExpression(el, parent)
            is PsiBinaryExpression -> JavaUBinaryExpression(el, parent)
            is PsiParenthesizedExpression -> JavaUParenthesizedExpression(el, parent)
            is PsiPrefixExpression -> JavaUPrefixExpression(el, parent)
            is PsiPostfixExpression -> JavaUPostfixExpression(el, parent)
            is PsiLiteralExpression -> JavaULiteralExpression(el, parent)
            is PsiReferenceExpression -> convertReference(el, parent)
            is PsiThisExpression -> JavaUThisExpression(el, parent)
            is PsiSuperExpression -> JavaUSuperExpression(el, parent)
            is PsiInstanceOfExpression -> JavaUInstanceCheckExpression(el, parent)
            is PsiTypeCastExpression -> JavaUTypeCastExpression(el, parent)
            is PsiClassObjectAccessExpression -> JavaUClassLiteralExpression(el, parent)
            is PsiArrayAccessExpression -> JavaUArrayAccessExpression(el, parent)
            is PsiLambdaExpression -> JavaULambdaExpression(el, parent)
            is PsiMethodReferenceExpression -> JavaUCallableReferenceExpression(el, parent)
            else -> UnknownJavaExpression(el, parent)
        }
    }
    
    internal fun convertStatement(el: PsiStatement, parent: UElement?): UExpression {
        getCached<UExpression>(el)?.let { return it }

        return when (el) {
            is PsiDeclarationStatement -> convertDeclarations(el.declaredElements, parent!!)
            is PsiExpressionListStatement -> convertDeclarations(el.expressionList.expressions, parent!!)
            is PsiBlockStatement -> JavaUBlockExpression(el, parent)
            is PsiLabeledStatement -> JavaULabeledExpression(el, parent)
            is PsiExpressionStatement -> convertExpression(el.expression, parent)
            is PsiIfStatement -> JavaUIfExpression(el, parent)
            is PsiSwitchStatement -> JavaUSwitchExpression(el, parent)
            is PsiSwitchLabelStatement -> {
                if (el.isDefaultCase)
                    DefaultUSwitchClauseExpression(parent)
                else JavaUCaseSwitchClauseExpression(el, parent)
            }
            is PsiWhileStatement -> JavaUWhileExpression(el, parent)
            is PsiDoWhileStatement -> JavaUDoWhileExpression(el, parent)
            is PsiForStatement -> JavaUForExpression(el, parent)
            is PsiForeachStatement -> JavaUForEachExpression(el, parent)
            is PsiBreakStatement -> JavaUBreakExpression(el, parent)
            is PsiContinueStatement -> JavaUContinueExpression(el, parent)
            is PsiReturnStatement -> JavaUReturnExpression(el, parent)
            is PsiAssertStatement -> JavaUAssertExpression(el, parent)
            is PsiThrowStatement -> JavaUThrowExpression(el, parent)
            is PsiSynchronizedStatement -> JavaUSynchronizedExpression(el, parent)
            is PsiTryStatement -> JavaUTryExpression(el, parent)
            else -> UnknownJavaExpression(el, parent)
        }
    }

    private fun convertDeclarations(elements: Array<out PsiElement>, parent: UElement): UVariableDeclarationsExpression {
        val languagePlugin = parent.getLanguagePlugin()
        return JavaUVariableDeclarationsExpression(parent).apply {
            val variables = mutableListOf<UVariable>()
            for (element in elements) {
                if (element !is PsiVariable) continue
                variables += SimpleUVariable.create(element, languagePlugin, this)
            }
            this.variables = variables
        }
    }

    internal fun convertOrEmpty(statement: PsiStatement?, parent: UElement?): UExpression {
        return statement?.let { convertStatement(it, parent) } ?: UastEmptyExpression
    }

    internal fun convertOrEmpty(expression: PsiExpression?, parent: UElement?): UExpression {
        return expression?.let { convertExpression(it, parent) } ?: UastEmptyExpression
    }

    internal fun convertOrNull(expression: PsiExpression?, parent: UElement?): UExpression? {
        return if (expression != null) convertExpression(expression, parent) else null
    }

    internal fun convertOrEmpty(block: PsiCodeBlock?, parent: UElement?): UExpression {
        return if (block != null) convertBlock(block, parent) else UastEmptyExpression
    }
}
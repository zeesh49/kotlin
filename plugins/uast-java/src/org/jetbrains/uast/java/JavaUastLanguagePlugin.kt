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
        
        val uElement = convertElementWithParent(e, null)
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
        
        val callExpression = convertElementWithParent(e, null) as? UCallExpression ?: return null
        
        val constructorMethod = e.resolveConstructor() ?: return null
        val containingClass = constructorMethod.containingClass ?: return null
        if (containingClass.qualifiedName != fqName) return null
        
        return Triple(callExpression, constructorMethod, containingClass)
    }

    override val priority = 0

    override fun isFileSupported(fileName: String): Boolean {
        return fileName.endsWith(".java", ignoreCase = true)
    }

    override fun convertElement(element: Any?, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        if (element !is PsiElement) return null
        return convertDeclaration(element, parent, requiredType) ?: JavaConverter.convertPsiElement(element, parent, requiredType)
    }

    override fun convertElementWithParent(element: Any?, requiredType: Class<out UElement>?): UElement? {
        if (element !is PsiElement) return null
        if (element is PsiJavaFile) return JavaUFile(element, this)
        JavaConverter.getCached<UElement>(element)?.let { return it }

        val parent = JavaConverter.unwrapParents(element.parent) ?: return null
        val parentUElement = convertElementWithParent(parent, null) ?: return null
        return convertElement(element, parentUElement, requiredType)
    }
    
    private fun convertDeclaration(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        return with (requiredType) { when (element) {
            is PsiJavaFile -> el<UFile> { JavaUFile(element, this@JavaUastLanguagePlugin) }
            is UDeclaration -> element
            is PsiClass -> el<UClass> { JavaUClass.create(element, this@JavaUastLanguagePlugin, parent) }
            is PsiMethod -> el<UMethod> { JavaUMethod.create(element, this@JavaUastLanguagePlugin, parent) }
            is PsiClassInitializer -> el<UClassInitializer> { JavaUClassInitializer(element, this@JavaUastLanguagePlugin, parent) }
            is PsiVariable -> el<UVariable> { JavaUVariable.create(element, this@JavaUastLanguagePlugin, parent) }
            is UAnnotation -> el<UAnnotation> { SimpleUAnnotation(element, this@JavaUastLanguagePlugin, parent) }
            else -> null
        }}
    }
}

internal inline fun <reified T : UElement> Class<out UElement>?.el(f: () -> UElement?): UElement? {
    return if (this == null || T::class.java == this) f() else null
}

internal inline fun <reified T : UElement> Class<out UElement>?.expr(f: () -> UExpression): UExpression {
    return if (this == null || T::class.java == this) f() else UastEmptyExpression
}

internal object JavaConverter {
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

    internal fun convertPsiElement(el: PsiElement, parent: UElement?, requiredType: Class<out UElement>? = null): UElement? {
        getCached<UElement>(el)?.let { return it }

        return with (requiredType) { when (el) {
            is PsiCodeBlock -> el<UBlockExpression> { convertBlock(el, parent) }
            is PsiResourceExpression -> convertExpression(el.expression, parent, requiredType)
            is PsiExpression -> convertExpression(el, parent, requiredType)
            is PsiStatement -> convertStatement(el, parent, requiredType)
            is PsiIdentifier -> el<USimpleNameReferenceExpression> { JavaUSimpleNameReferenceExpression(el, el.text, parent) }
            is PsiNameValuePair -> el<UNamedExpression> { convertNameValue(el, parent) }
            is PsiArrayInitializerMemberValue -> el<UCallExpression> { JavaAnnotationArrayInitializerUCallExpression(el, parent) }
            else -> null
        }}
    }
    
    internal fun convertBlock(block: PsiCodeBlock, parent: UElement?): UBlockExpression =
        getCached(block) ?: JavaUCodeBlockExpression(block, parent)

    internal fun convertNameValue(pair: PsiNameValuePair, parent: UElement?): UNamedExpression {
        return UNamedExpression.create(pair.name.orAnonymous(), parent) {
            val value = pair.value as? PsiElement
            value?.let { convertPsiElement(it, this, null) as? UExpression } ?: UnknownJavaExpression(value ?: pair, this)
        }
    }

    internal fun convertReference(expression: PsiReferenceExpression, parent: UElement?, requiredType: Class<out UElement>?): UExpression {
        return with (requiredType) {
            if (expression.isQualified) {
                expr<UQualifiedReferenceExpression> { JavaUQualifiedExpression(expression, parent) }
            } else {
                val name = expression.referenceName ?: "<error name>"
                expr<USimpleNameReferenceExpression> { JavaUSimpleNameReferenceExpression(expression, name, parent, expression) }
            }
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
    
    internal fun convertExpression(el: PsiExpression, parent: UElement?, requiredType: Class<out UElement>? = null): UExpression {
        getCached<UExpression>(el)?.let { return it }

        return with (requiredType) { when (el) {
            is PsiPolyadicExpression -> expr<UBinaryExpression> { convertPolyadicExpression(el, parent, el.operands.size - 1) }
            is PsiAssignmentExpression -> expr<UBinaryExpression> { JavaUAssignmentExpression(el, parent) }
            is PsiConditionalExpression -> expr<UIfExpression> { JavaUTernaryIfExpression(el, parent) }
            is PsiNewExpression -> {
                if (el.anonymousClass != null)
                    expr<UObjectLiteralExpression> { JavaUObjectLiteralExpression(el, parent) }
                else
                    expr<UCallExpression> { JavaConstructorUCallExpression(el, parent) }
            }
            is PsiMethodCallExpression -> {
                if (el.methodExpression.qualifierExpression != null)
                    expr<UQualifiedReferenceExpression> {
                        JavaUCompositeQualifiedExpression(el, parent).apply {
                            receiver = convertExpression(el.methodExpression.qualifierExpression!!, this)
                            selector = JavaUCallExpression(el, this)
                        }
                    }
                else
                    expr<UCallExpression> { JavaUCallExpression(el, parent) }
            }
            is PsiArrayInitializerExpression -> expr<UCallExpression> { JavaArrayInitializerUCallExpression(el, parent) }
            is PsiBinaryExpression -> expr<UBinaryExpression> { JavaUBinaryExpression(el, parent) }
            is PsiParenthesizedExpression -> expr<UParenthesizedExpression> { JavaUParenthesizedExpression(el, parent) }
            is PsiPrefixExpression -> expr<UPrefixExpression> { JavaUPrefixExpression(el, parent) }
            is PsiPostfixExpression -> expr<UPostfixExpression> { JavaUPostfixExpression(el, parent) }
            is PsiLiteralExpression -> expr<ULiteralExpression> { JavaULiteralExpression(el, parent) }
            is PsiReferenceExpression -> convertReference(el, parent, requiredType)
            is PsiThisExpression -> expr<UThisExpression> { JavaUThisExpression(el, parent) }
            is PsiSuperExpression -> expr<USuperExpression> { JavaUSuperExpression(el, parent) }
            is PsiInstanceOfExpression -> expr<UBinaryExpressionWithType> { JavaUInstanceCheckExpression(el, parent) }
            is PsiTypeCastExpression -> expr<UBinaryExpressionWithType> { JavaUTypeCastExpression(el, parent) }
            is PsiClassObjectAccessExpression -> expr<UClassLiteralExpression> { JavaUClassLiteralExpression(el, parent) }
            is PsiArrayAccessExpression -> expr<UArrayAccessExpression> { JavaUArrayAccessExpression(el, parent) }
            is PsiLambdaExpression -> expr<ULambdaExpression> { JavaULambdaExpression(el, parent) }
            is PsiMethodReferenceExpression -> expr<UCallableReferenceExpression> { JavaUCallableReferenceExpression(el, parent) }
            else -> UnknownJavaExpression(el, parent)
        }}
    }
    
    internal fun convertStatement(el: PsiStatement, parent: UElement?, requiredType: Class<out UElement>? = null): UExpression {
        getCached<UExpression>(el)?.let { return it }

        return when (el) {
            is PsiDeclarationStatement -> convertDeclarations(el.declaredElements, parent!!)
            is PsiExpressionListStatement -> convertDeclarations(el.expressionList.expressions, parent!!)
            is PsiBlockStatement -> JavaUBlockExpression(el, parent)
            is PsiLabeledStatement -> JavaULabeledExpression(el, parent)
            is PsiExpressionStatement -> convertExpression(el.expression, parent, requiredType)
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
                variables += JavaUVariable.create(element, languagePlugin, this)
            }
            this.variables = variables
        }
    }

    internal fun convertOrEmpty(statement: PsiStatement?, parent: UElement?): UExpression {
        return statement?.let { convertStatement(it, parent, null) } ?: UastEmptyExpression
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
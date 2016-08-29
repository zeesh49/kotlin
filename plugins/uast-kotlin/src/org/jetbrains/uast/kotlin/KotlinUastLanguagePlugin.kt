/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.uast.kotlin.expressions.KotlinUBreakExpression
import org.jetbrains.uast.kotlin.expressions.KotlinUContinueExpression
import org.jetbrains.uast.kotlin.kinds.KotlinSpecialExpressionKinds
import org.jetbrains.uast.*
import org.jetbrains.uast.java.JavaUClass
import org.jetbrains.uast.java.JavaUVariable
import org.jetbrains.uast.java.JavaUastLanguagePlugin
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable
import org.jetbrains.uast.psi.PsiElementBacked

interface KotlinUastBindingContextProviderService {
    fun getBindingContext(element: KtElement): BindingContext
    fun getTypeMapper(element: KtElement): KotlinTypeMapper?
}

class KotlinUastLanguagePlugin : UastLanguagePlugin() {
    override val priority = 10
    private val javaPlugin by lz { context.plugins.first { it is JavaUastLanguagePlugin } }

    override fun isFileSupported(fileName: String): Boolean {
        return fileName.endsWith(".kt", false) || fileName.endsWith(".kts", false)
    }

    override fun convertElement(element: Any?, parent: UElement?): UElement? {
        if (element !is PsiElement) return null
        return convertDeclaration(element, parent) ?: KotlinConverter.convertPsiElement(element, parent)
    }
    
    override fun convertWithParent(element: Any?): UElement? {
        if (element !is PsiElement) return null
        if (element is PsiFile) return convertDeclaration(element, null)

        val parent = element.parent ?: return null
        val parentUElement = convertWithParent(parent) ?: return null
        return convertElement(element, parentUElement)
    }

    override fun getMethodCallExpression(
            e: PsiElement, 
            containingClassFqName: String?, 
            methodName: String
    ): Pair<UCallExpression, PsiMethod>? {
        if (e !is KtCallExpression) return null
        val resolvedCall = e.getResolvedCall(e.analyze()) ?: return null
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is FunctionDescriptor || resultingDescriptor.name.asString() != methodName) return null
        
        val parent = e.parent ?: return null
        val parentUElement = convertWithParent(parent) ?: return null

        val uExpression = KotlinUFunctionCallExpression(e, parentUElement, resolvedCall)
        val method = uExpression.resolve() ?: return null
        if (method.name != methodName) return null
        return Pair(uExpression, method)
    }

    override fun getConstructorCallExpression(
            e: PsiElement, 
            fqName: String
    ): Triple<UCallExpression, PsiMethod, PsiClass>? {
        if (e !is KtCallExpression) return null
        val resolvedCall = e.getResolvedCall(e.analyze()) ?: return null
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is ConstructorDescriptor 
                || resultingDescriptor.returnType.constructor.declarationDescriptor?.name?.asString() != fqName) {
            return null
        }

        val parent = e.parent ?: return null
        val parentUElement = convertWithParent(parent) ?: return null

        val uExpression = KotlinUFunctionCallExpression(e, parentUElement, resolvedCall)
        val method = uExpression.resolve() ?: return null
        val containingClass = method.containingClass ?: return null
        return Triple(uExpression, method, containingClass)
    }

    private fun convertDeclaration(element: PsiElement, parent: UElement?): UElement? {
        if (element is UElement) return element
        
        val original = element.originalElement
        return when (original) {
            is KtLightMethod -> KotlinUMethod(original, this, parent)
            is KtLightClass -> KotlinUClass.create(original, this, parent)
            is KtLightField, is KtLightParameter -> KotlinUVariable.create(original as PsiVariable, this, parent)

            is KtClassOrObject -> original.toLightClass()?.let { lightClass -> KotlinUClass.create(lightClass, this, parent) }
            is KtFunction -> {
                val lightMethod = LightClassUtil.getLightClassMethod(original) ?: return null
                convertDeclaration(lightMethod, parent)
            }
            is KtPropertyAccessor -> javaPlugin.convertOpt(
                    LightClassUtil.getLightClassAccessorMethod(original), parent)
            is KtProperty -> javaPlugin.convertOpt<UField>(
                    LightClassUtil.getLightClassBackingField(original), parent)
                             ?: convertDeclaration(element.parent, parent)
            
            is KtFile -> KotlinUFile(original, this)
            is FakeFileForLightClass -> KotlinUFile(original.navigationElement, this)
            
            else -> null
        }
    }
}

internal object KotlinConverter : UastConverter {
    internal fun convertPsiElement(element: PsiElement?, parent: UElement?): UElement? = when (element) {
        is KtParameterList -> KotlinUVariableDeclarationsExpression(parent).apply {
            val languagePlugin = parent!!.getLanguagePlugin()
            variables = element.parameters.mapIndexed { i, p -> 
                KotlinUVariable.create(UastKotlinPsiParameter.create(p, element, i), languagePlugin, this)
            }
        }
        is KtClassBody -> KotlinUExpressionList(element, KotlinSpecialExpressionKinds.CLASS_BODY, parent).apply {
            expressions = emptyList()
        }
        is KtCatchClause -> KotlinUCatchClause(element, parent)
        is KtExpression -> KotlinConverter.convertExpression(element, parent)
        else -> {
            if (element is LeafPsiElement && element.elementType == KtTokens.IDENTIFIER) {
                asSimpleReference(element, parent)
            } else {
                null
            }
        }
    }
    
    private fun convertVariablesDeclaration(
            psi: KtVariableDeclaration, 
            parent: UElement?
    ): UVariableDeclarationsExpression {
        val languagePlugin = parent!!.getLanguagePlugin()
        val parentPsiElement = (parent as? PsiElementBacked)?.psi
        val variable = KotlinUVariable.create(UastKotlinPsiVariable.create(psi, parentPsiElement), languagePlugin, parent)
        return KotlinUVariableDeclarationsExpression(parent).apply { variables = listOf(variable) }
    }
    
    private fun convertStringTemplateExpression(
            expression: KtStringTemplateExpression,
            parent: UElement?,
            i: Int
    ): UExpression {
        return if (i == 1) KotlinStringTemplateUBinaryExpression(expression, parent).apply {
            leftOperand = convert(expression.entries[0], this)
            rightOperand = convert(expression.entries[1], this)
        } else KotlinStringTemplateUBinaryExpression(expression, parent).apply {
            leftOperand = convertStringTemplateExpression(expression, parent, i - 1)
            rightOperand = convert(expression.entries[i], this)
        }
    }

    internal fun convert(entry: KtStringTemplateEntry, parent: UElement?): UExpression = when (entry) {
        is KtStringTemplateEntryWithExpression -> convertOrEmpty(entry.expression, parent)
        is KtEscapeStringTemplateEntry -> KotlinStringULiteralExpression(entry, parent, entry.unescapedValue)
        else -> {
            KotlinStringULiteralExpression(entry, parent)
        }
    }

    internal fun convertExpression(expression: KtExpression, parent: UElement?): UExpression = when (expression) {
        is KtVariableDeclaration -> convertVariablesDeclaration(expression, parent)

        is KtStringTemplateExpression -> {
            if (expression.entries.isEmpty())
                KotlinStringULiteralExpression(expression, parent, "")
            else if (expression.entries.size == 1)
                convert(expression.entries[0], parent)
            else
                convertStringTemplateExpression(expression, parent, expression.entries.size - 1)
        }
        is KtDestructuringDeclaration -> KotlinUVariableDeclarationsExpression(parent).apply {
            val languagePlugin = parent!!.getLanguagePlugin()
            val tempAssignment = KotlinUVariable.create(UastKotlinPsiVariable.create(expression), languagePlugin, parent)
            val destructuringAssignments = expression.entries.mapIndexed { i, entry ->
                val psiFactory = KtPsiFactory(expression.project)
                val initializer = psiFactory.createExpression("${tempAssignment.name}.component${i + 1}()")
                KotlinUVariable.create(UastKotlinPsiVariable.create(
                        entry, tempAssignment.psi, initializer), languagePlugin, parent) 
            }
            variables = listOf(tempAssignment) + destructuringAssignments
        }
        is KtLabeledExpression -> KotlinULabeledExpression(expression, parent)
        is KtClassLiteralExpression -> KotlinUClassLiteralExpression(expression, parent)
        is KtObjectLiteralExpression -> KotlinUObjectLiteralExpression(expression, parent)
        is KtStringTemplateEntry -> convertOrEmpty(expression.expression, parent)
        is KtDotQualifiedExpression -> KotlinUQualifiedReferenceExpression(expression, parent)
        is KtSafeQualifiedExpression -> KotlinUSafeQualifiedExpression(expression, parent)
        is KtSimpleNameExpression -> KotlinUSimpleReferenceExpression(expression, expression.getReferencedName(), parent)
        is KtCallExpression -> KotlinUFunctionCallExpression(expression, parent)
        is KtBinaryExpression -> KotlinUBinaryExpression(expression, parent)
        is KtParenthesizedExpression -> KotlinUParenthesizedExpression(expression, parent)
        is KtPrefixExpression -> KotlinUPrefixExpression(expression, parent)
        is KtPostfixExpression -> KotlinUPostfixExpression(expression, parent)
        is KtThisExpression -> KotlinUThisExpression(expression, parent)
        is KtSuperExpression -> KotlinUSuperExpression(expression, parent)
        is KtCallableReferenceExpression -> KotlinUCallableReferenceExpression(expression, parent)
        is KtIsExpression -> KotlinUTypeCheckExpression(expression, parent)
        is KtIfExpression -> KotlinUIfExpression(expression, parent)
        is KtWhileExpression -> KotlinUWhileExpression(expression, parent)
        is KtDoWhileExpression -> KotlinUDoWhileExpression(expression, parent)
        is KtForExpression -> KotlinUForEachExpression(expression, parent)
        is KtWhenExpression -> KotlinUSwitchExpression(expression, parent)
        is KtBreakExpression -> KotlinUBreakExpression(expression, parent)
        is KtContinueExpression -> KotlinUContinueExpression(expression, parent)
        is KtReturnExpression -> KotlinUReturnExpression(expression, parent)
        is KtThrowExpression -> KotlinUThrowExpression(expression, parent)
        is KtBlockExpression -> KotlinUBlockExpression(expression, parent)
        is KtConstantExpression -> KotlinULiteralExpression(expression, parent)
        is KtTryExpression -> KotlinUTryExpression(expression, parent)
        is KtArrayAccessExpression -> KotlinUArrayAccessExpression(expression, parent)
        is KtLambdaExpression -> KotlinULambdaExpression(expression, parent)
        is KtBinaryExpressionWithTypeRHS -> KotlinUBinaryExpressionWithType(expression, parent)

        else -> UnknownKotlinExpression(expression, parent)
    }

    internal fun asSimpleReference(element: PsiElement?, parent: UElement?): USimpleNameReferenceExpression? {
        if (element == null) return null
        return KotlinNameUSimpleReferenceExpression(element, KtPsiUtil.unquoteIdentifier(element.text), parent)
    }

    internal fun convertOrEmpty(expression: KtExpression?, parent: UElement?): UExpression {
        return if (expression != null) convertExpression(expression, parent) else UastEmptyExpression
    }

    internal fun convertOrNull(expression: KtExpression?, parent: UElement?): UExpression? {
        return if (expression != null) convertExpression(expression, parent) else null
    }
}
/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.usageView.UsageViewShortNameLocation
import com.intellij.xml.breadcrumbs.BreadcrumbsInfoProvider
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses
import kotlin.reflect.KClass

class KotlinBreadcrumbsInfoProvider : BreadcrumbsInfoProvider() {
    private abstract class ElementHandler<TElement : KtElement>(val type: KClass<TElement>) {
        abstract fun elementInfo(element: TElement): String
        abstract fun elementTooltip(element: TElement): String

        open fun accepts(element: TElement): Boolean = true
    }

    private val handlers = listOf<ElementHandler<*>>(
            LambdaHandler,
            AnonymousObjectHandler,
            AnonymousFunctionHandler,
            PropertyAccessorHandler,
            DeclarationHandler,
            IfHandler,
            ElseHandler,
            TryHandler,
            CatchHandler,
            FinallyHandler,
            WhileHandler,
            DoWhileHandler,
            WhenHandler,
            WhenEntryHandler,
            ForHandler
    )

    private object LambdaHandler : ElementHandler<KtFunctionLiteral>(KtFunctionLiteral::class) {
        override fun elementInfo(element: KtFunctionLiteral): String {
            val lambdaExpression = element.parent as KtLambdaExpression

            val parent = lambdaExpression.parent
            when (parent) {
                is KtLambdaArgument -> {
                    val callExpression = parent.parent as? KtCallExpression
                    val callName = callExpression?.getCallNameExpression()?.getReferencedName()
                    if (callName != null) {
                        val receiverText = callExpression.getQualifiedExpressionForSelector()?.let {
                            it.receiverExpression.text.orElipsis(TextKind.INFO) + it.operationSign.value
                        } ?: ""

                        var argumentText = "($elipsis)"
                        if (callExpression.valueArgumentList != null) {
                            val arguments = callExpression.getValueArgumentsInParentheses()
                            when (arguments.size) {
                                0 -> argumentText = "()"
                                1 -> {
                                    val argument = arguments.single()
                                    val argumentExpression = argument.getArgumentExpression()
                                    if (!argument.isNamed() && argument.getSpreadElement() == null && argumentExpression != null) {
                                        argumentText = "(" + argumentExpression.shortText(TextKind.INFO) + ")"
                                    }
                                }
                            }
                            return "$receiverText$callName$argumentText {$elipsis}"
                        }
                        else {
                            return "$receiverText$callName{$elipsis}"
                        }
                    }
                }

                is KtProperty -> {
                    val name = parent.nameAsName
                    if (lambdaExpression == parent.initializer && name != null) {
                        val valOrVar = if (parent.isVar) "var" else "val"
                        return valOrVar + " " + name.render() + " = {$elipsis}"
                    }
                }
            }

            return "{$elipsis}"
        }

        //TODO
        override fun elementTooltip(element: KtFunctionLiteral): String {
            return ElementDescriptionUtil.getElementDescription(element, RefactoringDescriptionLocation.WITH_PARENT)
        }
    }

    private object AnonymousObjectHandler : ElementHandler<KtObjectDeclaration>(KtObjectDeclaration::class) {
        override fun accepts(element: KtObjectDeclaration) = element.isObjectLiteral()

        override fun elementInfo(element: KtObjectDeclaration) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtObjectDeclaration) = element.buildText(TextKind.TOOLTIP)

        private fun KtObjectDeclaration.buildText(kind: TextKind): String {
            return buildString {
                append("object")

                val superTypeEntries = getSuperTypeListEntries()
                if (superTypeEntries.isNotEmpty()) {
                    append(" : ")

                    if (kind == TextKind.INFO) {
                        val entry = superTypeEntries.first()
                        entry.typeReference?.text?.truncateStart(kind)?.let { append(it) }
                        if (superTypeEntries.size > 1) {
                            if (!endsWith(elipsis)) {
                                append(",${elipsis}")
                            }
                        }
                    }
                    else {
                        append(superTypeEntries.joinToString(separator = ", ") { it.typeReference?.text ?: "" }.truncateEnd(kind))
                    }
                }
            }
        }
    }

    private object AnonymousFunctionHandler : ElementHandler<KtNamedFunction>(KtNamedFunction::class) {
        override fun accepts(element: KtNamedFunction) = element.name == null

        override fun elementInfo(element: KtNamedFunction) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtNamedFunction) = element.buildText(TextKind.TOOLTIP)

        private fun KtNamedFunction.buildText(kind: TextKind): String {
            return "fun(" +
                   valueParameters.joinToString(separator = ", ") { if (kind == TextKind.INFO) it.name ?: "" else it.text }.truncateEnd(kind) +
                   ")"
        }
    }

    private object PropertyAccessorHandler : ElementHandler<KtPropertyAccessor>(KtPropertyAccessor::class) {
        override fun elementInfo(element: KtPropertyAccessor): String {
            return element.property.name + "." + (if (element.isGetter) "get" else "set")
        }

        override fun elementTooltip(element: KtPropertyAccessor): String {
            return DeclarationHandler.elementTooltip(element)
        }
    }

    private object DeclarationHandler : ElementHandler<KtDeclaration>(KtDeclaration::class) {
        override fun accepts(element: KtDeclaration): Boolean {
            return element !is KtProperty
        }

        override fun elementInfo(element: KtDeclaration): String {
            val description = ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
            val suffix = if (element is KtFunction) "()" else null
            return if (suffix != null) description + suffix else description
        }

        override fun elementTooltip(element: KtDeclaration): String {
            return ElementDescriptionUtil.getElementDescription(element, RefactoringDescriptionLocation.WITH_PARENT)
        }
    }

    private abstract class ConstructWithExpressionHandler<TExpression : KtExpression>(
            private val constructName: String,
            type: KClass<TExpression>
    ) : ElementHandler<TExpression>(type) {
        protected abstract fun extractExpression(construct: TExpression): KtExpression?

        override fun elementInfo(element: TExpression) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: TExpression) = element.buildText(TextKind.TOOLTIP)

        protected fun TExpression.buildText(kind: TextKind): String {
            val expression = extractExpression(this) ?: return constructName
            return "$constructName (${expression.shortText(kind)})"
        }
    }

    private object IfHandler : ConstructWithExpressionHandler<KtIfExpression>("if", KtIfExpression::class) {
        override fun extractExpression(construct: KtIfExpression) = construct.condition

        override fun elementInfo(element: KtIfExpression): String {
            return elseIfPrefix(element) + super.elementInfo(element)
        }

        override fun elementTooltip(element: KtIfExpression): String {
            return elseIfPrefix(element) + super.elementTooltip(element)
        }

        private fun elseIfPrefix(element: KtIfExpression): String {
            return if (element.isElseIf()) "if $elipsis else " else ""
        }
    }

    private object ElseHandler : ElementHandler<KtContainerNode>(KtContainerNode::class) {
        override fun accepts(element: KtContainerNode): Boolean {
            return element.node.elementType == KtNodeTypes.ELSE
                   && (element.parent as KtIfExpression).`else` !is KtIfExpression // filter out "else if"
        }

        override fun elementInfo(element: KtContainerNode): String {
            val ifExpression = element.parent as KtIfExpression
            val ifInfo = if (ifExpression.isElseIf()) "if" else IfHandler.elementInfo(ifExpression)
            return "$ifInfo $elipsis else"
        }

        override fun elementTooltip(element: KtContainerNode): String {
            val ifExpression = element.parent as KtIfExpression
            return "else (of '" + IfHandler.elementTooltip(ifExpression) + "')" //TODO
        }
    }

    private object TryHandler : ElementHandler<KtTryExpression>(KtTryExpression::class) {
        override fun elementInfo(element: KtTryExpression) = "try"
        override fun elementTooltip(element: KtTryExpression) = "try"
    }

    private object CatchHandler : ElementHandler<KtCatchClause>(KtCatchClause::class) {
        override fun elementInfo(element: KtCatchClause): String {
            val text = element.catchParameter?.text ?: ""
            return "catch ($text)"
        }

        override fun elementTooltip(element: KtCatchClause): String {
            return elementInfo(element)
        }
    }

    private object FinallyHandler : ElementHandler<KtFinallySection>(KtFinallySection::class) {
        override fun elementInfo(element: KtFinallySection) = "finally"
        override fun elementTooltip(element: KtFinallySection) = "finally"
    }

    private object WhileHandler : ConstructWithExpressionHandler<KtWhileExpression>("while", KtWhileExpression::class) {
        override fun extractExpression(construct: KtWhileExpression) = construct.condition
    }

    private object DoWhileHandler : ConstructWithExpressionHandler<KtDoWhileExpression>("do $elipsis while", KtDoWhileExpression::class) {
        override fun extractExpression(construct: KtDoWhileExpression) = construct.condition
    }

    private object WhenHandler : ConstructWithExpressionHandler<KtWhenExpression>("when", KtWhenExpression::class) {
        override fun extractExpression(construct: KtWhenExpression) = construct.subjectExpression
    }

    private object WhenEntryHandler : ElementHandler<KtWhenEntry>(KtWhenEntry::class) {
        override fun elementInfo(element: KtWhenEntry) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtWhenEntry) = element.buildText(TextKind.TOOLTIP)

        private fun KtWhenEntry.buildText(kind: TextKind): String {
            if (isElse) {
                return "else ->"
            }
            else {
                val condition = conditions.firstOrNull() ?: return "->"
                val firstConditionText = when (condition) {
                    is KtWhenConditionIsPattern -> {
                        (if (condition.isNegated) "!is" else "is") + " " + (condition.typeReference?.text?.truncateEnd(kind) ?: "")
                    }

                    is KtWhenConditionInRange -> {
                        (if (condition.isNegated) "!in" else "in") + " " + (condition.rangeExpression?.text?.truncateEnd(kind) ?: "")
                    }

                    is KtWhenConditionWithExpression -> {
                        condition.expression?.text?.truncateStart(kind) ?: ""
                    }

                    else -> error("Unknown when entry condition type: $condition")
                }

                if (conditions.size == 1) {
                    return firstConditionText + " ->"
                }
                else {
                    //TODO: show all conditions for !shortText
                    return (if (firstConditionText.endsWith(elipsis)) firstConditionText else firstConditionText + ",$elipsis") + " ->"
                }
            }
        }
    }

    private object ForHandler : ElementHandler<KtForExpression>(KtForExpression::class) {
        override fun elementInfo(element: KtForExpression) = element.buildText(TextKind.INFO)
        override fun elementTooltip(element: KtForExpression) = element.buildText(TextKind.TOOLTIP)

        private fun KtForExpression.buildText(kind: TextKind): String {
            val parameterText = loopParameter?.nameAsName?.render() ?: destructuringParameter?.text ?: return "for"
            val collectionText = loopRange?.text ?: ""
            val text = (parameterText + " in " + collectionText).truncateEnd(kind)
            return "for($text)"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handler(e: PsiElement): ElementHandler<in KtElement>? {
        if (e !is KtElement) return null
        val handler = handlers.firstOrNull { it.type.java.isInstance(e) && (it as ElementHandler<in KtElement>).accepts(e) }
        return handler as ElementHandler<in KtElement>?
    }

    override fun getLanguages() = arrayOf(KotlinLanguage.INSTANCE)

    override fun acceptElement(e: PsiElement) = handler(e) != null

    override fun getElementInfo(e: PsiElement): String {
        return handler(e)!!.elementInfo(e as KtElement)
    }

    override fun getElementTooltip(e: PsiElement): String {
        return handler(e)!!.elementTooltip(e as KtElement)
    }

    override fun getParent(e: PsiElement): PsiElement? {
        val node = e.node ?: return null
        when (node.elementType) {
            KtNodeTypes.ELSE, KtNodeTypes.CATCH, KtNodeTypes.FINALLY -> return e.parent.parent
            else -> return e.parent
        }
    }

    private companion object {
        enum class TextKind {
            INFO, TOOLTIP
        }

        fun KtExpression.shortText(kind: TextKind) = if (this is KtNameReferenceExpression) text else text.truncateEnd(kind)

        fun maxLength(kind: TextKind): Int {
            return when (kind) {
                TextKind.INFO -> 16
                TextKind.TOOLTIP -> 100
            }
        }

        //TODO: line breaks

        fun String.orElipsis(kind: TextKind): String {
            val maxLength = maxLength(kind)
            return if (length <= maxLength) this else elipsis
        }

        fun String.truncateEnd(kind: TextKind): String {
            val maxLength = maxLength(kind)
            return if (length > maxLength) substring(0, maxLength - elipsis.length) + elipsis else this
        }

        fun String.truncateStart(kind: TextKind): String {
            val maxLength = maxLength(kind)
            return if (length > maxLength) elipsis + substring(length - maxLength - 1) else this
        }

        val elipsis = "\u2026"

        fun KtIfExpression.isElseIf() = parent.node.elementType == KtNodeTypes.ELSE
    }
}

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
package org.jetbrains.uast

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiResourceListElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.expressions.UTypeReferenceExpression
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.internal.log
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents
 *
 * `try {
 *      // tryClause body
 *  } catch (e: Type1, Type2 ... TypeN) {
 *      // catchClause1 body
 *  } ... {
 *  finally {
 *      //finallyBody
 *  }`
 *
 *  and
 *
 *  `try (resource1, ..., resourceN) {
 *      // tryClause body
 *  }`
 *
 *  expressions.
 */
interface UTryExpression : UExpression {
    /**
     * Returns the list of try resources, or null if this expression is not a `try-with-resources` expression.
     */
    val resources: List<PsiResourceListElement>?
    
    val isResources: Boolean

    /**
     * Returns the `try` clause expression.
     */
    val tryClause: UExpression

    /**
     * Returns the `catch` clauses [UCatchClause] expression list.
     */
    val catchClauses: List<UCatchClause>

    /**
     * Returns the `finally` clause expression, or null if the `finally` clause is absent.
     */
    val finallyClause: UExpression?

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitTryExpression(this)) return
        tryClause.accept(visitor)
        catchClauses.acceptList(visitor)
        finallyClause?.accept(visitor)
        visitor.afterVisitTryExpression(this)
    }

    override fun renderString() = buildString {
        append("try ")
        appendln(tryClause.renderString().trim('\n', '\r'))
        catchClauses.forEach { appendln(it.renderString().trim('\n', '\r')) }
        finallyClause?.let { append("finally ").append(it.renderString().trim('\n', '\r')) }
    }

    override fun logString() = log("UTryExpression", tryClause, catchClauses, finallyClause)
}

/**
 * Represents the `catch` clause in [UTryExpression].
 */
interface UCatchClause : UElement {
    /**
     * Returns the `catch` clause body expression.
     */
    val body: UExpression

    /**
     * Returns the exception parameter variables for this `catch` clause.
     */
    val parameters: List<UParameter>

    /**
     * Returns the exception types for this `catch` clause.
     */
    val typeReferences: List<UTypeReferenceExpression>
    
    val types: List<PsiType>
        get() = typeReferences.map { it.type }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitCatchClause(this)) return
        body.accept(visitor)
        visitor.afterVisitCatchClause(this)
    }

    override fun logString() = log("UCatchClause", body)
    override fun renderString() = "catch (e) " + body.renderString()
}
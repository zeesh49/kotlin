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

package org.jetbrains.kotlin.ir2cfg.generators

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlock
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlockBuilder
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlockImpl
import org.jetbrains.kotlin.utils.toReadOnlyList

class BasicBlockGenerator : IrElementVisitor<IrElement?, Nothing?> {

    val builder = object: BasicBlockBuilder {

        val elements = arrayListOf<IrElement>()

        override fun add(element: IrElement) {
            elements.add(element)
        }

        override fun add(basicBlock: BasicBlock) {
            elements.addAll(basicBlock.elements)
        }

        override fun build() = BasicBlockImpl(elements.toReadOnlyList())
    }

    fun build() = builder.build()

    // All visits return element if it's the end of basic block, otherwise return null

    override fun visitElement(element: IrElement, data: Nothing?) = element

    override fun visitExpression(expression: IrExpression, data: Nothing?): IrElement? {
        builder.add(expression)
        return null
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?): IrElement? {
        for (statement in expression.statements) {
            statement.accept(this, data) ?: continue
            return statement
        }
        return null
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): IrElement? {
        builder.add(declaration)
        return null
    }
}
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

package org.jetbrains.kotlin.ir2cfg.traverse

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction

class IrFunctionTraverser(function: IrFunction) : AbstractIrTraverser() {

    private val nextEdgesCache = mutableMapOf<IrElement, Edges>()

    private val previousEdgesCache = mutableMapOf<IrElement, Edges>()

    private var currentEdges: Edges = startEdges(function)

    override val previousElements: List<IrElement>
        get() = currentEdges.previous

    override val nextElements: List<IrElement>
        get() = currentEdges.next

    override fun onNext(next: IrElement) {
        previousIndex++
        nextIndex++
        currentEdges = nextEdgesCache[next] ?: nextEdges(next).apply { nextEdgesCache[next] = this }
    }

    private var nextIndex = 0

    override fun nextIndex() = nextIndex

    override fun onPrevious(previous: IrElement) {
        previousIndex--
        nextIndex--
        currentEdges = previousEdgesCache[previous] ?: previousEdges(previous).apply { previousEdgesCache[previous] = this }
    }

    private var previousIndex = -1

    override fun previousIndex() = previousIndex

    class Edges(val previous: List<IrElement>, val next: List<IrElement>) {
        constructor(previous: IrElement, next: IrElement): this(listOf(previous), listOf(next))

        constructor(previous: IrElement, next: List<IrElement>): this(listOf(previous), next)

        constructor(previous: List<IrElement>, next: IrElement): this(previous, listOf(next))
    }

    companion object {
        val noEdges = Edges(listOf(), listOf())

        fun startEdges(vararg next: IrElement) = Edges(listOf(), next.toList())

        fun endEdges(vararg previous: IrElement) = Edges(previous.toList(), listOf())

        fun nextEdges(previous: IrElement): Edges {
            TODO()
        }

        fun previousEdges(next: IrElement): Edges {
            TODO()
        }
    }
}
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

abstract class AbstractIrTraverser : IrTraverser {

    private val visitedPrevious = mutableSetOf<IrElement>()

    private val visitedNext = mutableSetOf<IrElement>()

    override final val defaultPrevious: IrElement?
        get() = previousElements.firstOrNull { it !in visitedPrevious }

    override final val defaultNext: IrElement?
        get() = nextElements.firstOrNull { it !in visitedNext }

    override final fun hasNext() = defaultNext != null

    abstract protected fun onNext(next: IrElement)

    override final fun next(to: IrElement): IrElement {
        if (to !in nextElements || to in visitedNext) throw NoSuchElementException()
        visitedNext.add(to)
        visitedPrevious.remove(to)
        onNext(to)
        return to
    }

    override final fun next() = next(defaultNext ?: throw NoSuchElementException())

    override final fun hasPrevious() = defaultPrevious != null

    abstract protected fun onPrevious(previous: IrElement)

    override final fun previous(to: IrElement): IrElement {
        if (to !in previousElements || to in visitedPrevious) throw NoSuchElementException()
        visitedPrevious.add(to)
        visitedNext.remove(to)
        onPrevious(to)
        return to
    }

    override final fun previous() = previous(defaultPrevious ?: throw NoSuchElementException())
}
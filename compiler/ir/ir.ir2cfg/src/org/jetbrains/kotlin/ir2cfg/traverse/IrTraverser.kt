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

interface IrTraverser : ListIterator<IrElement> {
    // default previous element, null iff at start
    val defaultPrevious: IrElement?

    // list of back edges (empty if we're at start)
    val previousElements: List<IrElement>

    // default next element, null iff at end
    val defaultNext: IrElement?

    // list of forward edges (empty if we're at end)
    val nextElements: List<IrElement>

    // false if we're at end
    override fun hasNext(): Boolean

    // traverse to given (must be in nextElements) next element, throws exception otherwise
    fun next(to: IrElement): IrElement

    // traverse to default next element, throws exception otherwise
    override fun next(): IrElement

    // get index of default next element
    override fun nextIndex(): Int

    // false if we're at start
    override fun hasPrevious(): Boolean

    // traverse to given (must be in previousElements) previous element, throws exception otherwise
    fun previous(to: IrElement): IrElement

    // traverse to default previous element, throws exception otherwise
    override fun previous(): IrElement

    // get index of default previous element
    override fun previousIndex(): Int
}

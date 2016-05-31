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

package org.jetbrains.kotlin.codegen.optimization.fixStack

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.kotlin.codegen.inline.MaxLocalsCalculator
import org.jetbrains.kotlin.codegen.inline.MaxStackFrameSizeAndLocalsCalculator
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.*
import java.util.*

//TODO consider to merge this logic into FixStackMethodTransformer
class CleanStackOnReturnTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val returns = InsnSequence(methodNode.instructions).filter { insnNode ->
            InlineCodegenUtil.isReturnOpcode(insnNode.opcode)
        }.toList()

        if (returns.size <= 1) {
            return
        }

        val maxLocalsCalculator = MaxLocalsCalculator(Opcodes.ASM5, methodNode.access, methodNode.desc, null)
        methodNode.accept(maxLocalsCalculator)

        val analyzer = object : Analyzer<BasicValue>(BasicInterpreter()) {
            override fun newFrame(nLocals: Int, nStack: Int): Frame<BasicValue> {
                return object : Frame<BasicValue>(nLocals, nStack) {
                    @Throws(AnalyzerException::class)
                    override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<BasicValue>) {
                        // This can be a void non-local return from a non-void method; Frame#execute would throw and do nothing else.
                        if (insn.opcode == Opcodes.RETURN) return
                        super.execute(insn, interpreter)
                    }
                }
            }
        }

        val frames = analyzer.analyze(internalClassName, methodNode)
        returns.reversed().forEach { insn ->
            val frame = frames[methodNode.instructions.indexOf(insn)] ?: return@forEach
            val returnSize = returnType(insn.opcode)
            val insertBeforeInsn = if (InlineCodegenUtil.isMarkedReturn(insn)) insn.previous else insn
            methodNode.cleanStack(insertBeforeInsn, frame, returnSize, maxLocalsCalculator.maxLocals)
        }
        methodNode.accept(maxLocalsCalculator)
        methodNode.maxLocals = maxLocalsCalculator.maxLocals
    }

    private fun MethodNode.cleanStack(insBeforeInsn: AbstractInsnNode, frame: Frame<BasicValue>, returnType: Type, maxLocals: Int) {
        val valueSizes = ((if (returnType == Type.VOID_TYPE) 0 else 1)..frame.stackSize - 1).map {
            frame.peek(it)!!.type.size
        }
        if (valueSizes.isEmpty()) return

        // merge single slot primitives in two slot ones
        val mergedSizes = ArrayList<Int>(valueSizes.size)
        var prev = 0
        for (i in valueSizes) {
            if (i + prev > 2) {
                mergedSizes.add(prev)
                prev = 0
            }
            prev += i
        }
        if (prev != 0) mergedSizes.add(prev)

        val methodNodeStub = MethodNode()
        val stub = InstructionAdapter(methodNodeStub)
        val toRemoveSize = valueSizes.sum()
        val useSwap = toRemoveSize == 1 && returnType.size == 1
        if (returnType == Type.VOID_TYPE) {
            //do nothing
        }
        else if (useSwap) {
            //TODO: for 2 sized values we need to update maxStack
            AsmUtil.swap(stub, returnType.size, toRemoveSize)
        }
        else {
            stub.store(maxLocals, returnType)
        }

        mergedSizes.forEach { valueSize ->
            if (valueSize == 2) stub.pop2() else stub.pop()
        }

        //SEE TODO above
        if (!useSwap && returnType != Type.VOID_TYPE) {
            stub.load(maxLocals, returnType)
        }
        InlineCodegenUtil.insertNodeBefore(methodNodeStub, this, insBeforeInsn)
    }


    private fun returnType(opcode: Int): Type {
        return when (opcode) {
            Opcodes.RETURN -> Type.VOID_TYPE
            Opcodes.IRETURN -> Type.INT_TYPE
            Opcodes.FRETURN -> Type.FLOAT_TYPE
            Opcodes.ARETURN -> AsmTypes.OBJECT_TYPE
            Opcodes.LRETURN -> Type.LONG_TYPE;
            Opcodes.DRETURN -> Type.DOUBLE_TYPE;
            else -> throw RuntimeException("Unsupported opcode $opcode")
        }
    }

}
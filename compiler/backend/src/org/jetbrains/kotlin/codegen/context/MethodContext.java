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

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.JvmCodegenUtil;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;

public class MethodContext extends CodegenContext<CallableMemberDescriptor> {

    private final Kind kind;

    public enum Kind {
        DEFAULT,
        INLINING_LAMBDA,
        INLINING_FUN_IN_SAME_CLASS
    }

    private Label methodStartLabel;
    private Label methodEndLabel;

    // Note: in case of code inside property accessors, functionDescriptor will be that accessor,
    // but CodegenContext#contextDescriptor will be the corresponding property
    private final FunctionDescriptor functionDescriptor;

    protected MethodContext(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind contextKind,
            @NotNull CodegenContext parentContext,
            @Nullable MutableClosure closure,
            Kind kind
    ) {
        super(JvmCodegenUtil.getDirectMember(functionDescriptor), contextKind, parentContext, closure,
              parentContext.hasThisDescriptor() ? parentContext.getThisDescriptor() : null, null);
        this.kind = kind;
        this.functionDescriptor = functionDescriptor;
    }

    @NotNull
    @Override
    public CodegenContext getParentContext() {
        //noinspection ConstantConditions
        return super.getParentContext();
    }

    public StackValue getReceiverExpression(JetTypeMapper typeMapper) {
        assert getCallableDescriptorWithReceiver() != null;
        @SuppressWarnings("ConstantConditions")
        Type asmType = typeMapper.mapType(getCallableDescriptorWithReceiver().getExtensionReceiverParameter().getType());
        return StackValue.local(AsmUtil.getReceiverIndex(this, getContextDescriptor()), asmType);
    }

    @Override
    public StackValue lookupInContext(DeclarationDescriptor d, @Nullable StackValue result, GenerationState state, boolean ignoreNoOuter) {
        if (getContextDescriptor() == d) {
            return result != null ? result : StackValue.LOCAL_0;
        }

        return getParentContext().lookupInContext(d, result, state, ignoreNoOuter);
    }

    @Nullable
    public StackValue generateReceiver(@NotNull CallableDescriptor descriptor, @NotNull GenerationState state, boolean ignoreNoOuter) {
        if (getCallableDescriptorWithReceiver() == descriptor) {
            return getReceiverExpression(state.getTypeMapper());
        }
        ReceiverParameterDescriptor parameter = descriptor.getExtensionReceiverParameter();
        return lookupInContext(parameter, StackValue.LOCAL_0, state, ignoreNoOuter);
    }

    @Override
    public StackValue getOuterExpression(StackValue prefix, boolean ignoreNoOuter) {
        return getParentContext().getOuterExpression(prefix, false);
    }

    @Nullable
    public Label getMethodStartLabel() {
        return methodStartLabel;
    }

    public void setMethodStartLabel(@NotNull Label methodStartLabel) {
        this.methodStartLabel = methodStartLabel;
    }

    @Nullable
    public Label getMethodEndLabel() {
        return methodEndLabel;
    }

    public void setMethodEndLabel(@NotNull Label methodEndLabel) {
        this.methodEndLabel = methodEndLabel;
    }

    @Override
    public String toString() {
        return "Method: " + getContextDescriptor();
    }

    public boolean isInlineFunction() {
        return InlineUtil.isInline(getContextDescriptor());
    }

    public boolean isInliningLambda() {
        return kind == Kind.INLINING_LAMBDA;
    }

    @NotNull
    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }

    @Override
    protected boolean generateAccessorInAnyCase(
            @NotNull CallableDescriptor descriptor, @NotNull CodegenContext fromContext
    ) {
        if (isInlineFunction()) {
            if (kind == Kind.DEFAULT) {
                return true;
            }
        }
        return false;
    }
}

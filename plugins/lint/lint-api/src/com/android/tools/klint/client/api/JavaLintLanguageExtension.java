/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.klint.client.api;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiVariable;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastContext;
import org.jetbrains.uast.java.JavaUastLanguagePlugin;

import kotlin.Pair;
import kotlin.Triple;

public class JavaLintLanguageExtension extends LintLanguageExtension {
    private final JavaUastLanguagePlugin delegate = new JavaUastLanguagePlugin();

    @Override
    public void setContext(UastContext uastContext) {
        delegate.setContext(uastContext);
    }

    @Override
    public boolean isFileSupported(String fileName) {
        return delegate.isFileSupported(fileName);
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }

    @Nullable
    @Override
    public UElement convertElement(Object element, UElement parent) {
        return delegate.convertElement(element, parent);
    }

    @Nullable
    @Override
    public UElement convertWithParent(Object element) {
        return delegate.convertWithParent(element);
    }

    @Nullable
    @Override
    public Pair<UCallExpression, PsiMethod> getMethodCallExpression(
            PsiElement element, String containingClassFqName, String methodName) {
        return delegate.getMethodCallExpression(element, containingClassFqName, methodName);
    }

    @Nullable
    @Override
    public Triple<UCallExpression, PsiMethod, PsiClass> getConstructorCallExpression(
            PsiElement element, String classFqName) {
        return delegate.getConstructorCallExpression(element, classFqName);
    }

    @Nullable
    @Override
    public UExpression getMethodBody(PsiMethod method) {
        return delegate.getMethodBody(method);
    }

    @Nullable
    @Override
    public UExpression getInitializerBody(PsiVariable variable) {
        return delegate.getInitializerBody(variable);
    }

    @Nullable
    @Override
    public UExpression getInitializerBody(PsiClassInitializer psiClassInitializer) {
        return delegate.getInitializerBody(psiClassInitializer);
    }
}

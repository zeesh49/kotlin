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

package org.jetbrains.kotlin.js.translate.utils;

import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.js.PredefinedAnnotation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.source.PsiSourceFile;
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptPackageFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class AnnotationsUtils {
    private static final FqName JS_MODULE_ANNOTATION = new FqName("kotlin.js.JsModule");
    private static final FqName JS_NON_MODULE_ANNOTATION = new FqName("kotlin.js.JsNonModule");

    private AnnotationsUtils() {
    }

    public static boolean hasAnnotation(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull PredefinedAnnotation annotation
    ) {
        return getAnnotationByName(descriptor, annotation) != null;
    }

    @Nullable
    private static String getAnnotationStringParameter(@NotNull DeclarationDescriptor declarationDescriptor,
            @NotNull PredefinedAnnotation annotation) {
        AnnotationDescriptor annotationDescriptor = getAnnotationByName(declarationDescriptor, annotation);
        assert annotationDescriptor != null;
        //TODO: this is a quick fix for unsupported default args problem
        if (annotationDescriptor.getAllValueArguments().isEmpty()) {
            return null;
        }
        ConstantValue<?> constant = annotationDescriptor.getAllValueArguments().values().iterator().next();
        //TODO: this is a quick fix for unsupported default args problem
        if (constant == null) {
            return null;
        }
        Object value = constant.getValue();
        assert value instanceof String : "Native function annotation should have one String parameter";
        return (String) value;
    }

    @Nullable
    public static String getNameForAnnotatedObject(@NotNull DeclarationDescriptor declarationDescriptor,
            @NotNull PredefinedAnnotation annotation) {
        if (!hasAnnotation(declarationDescriptor, annotation)) {
            return null;
        }
        return getAnnotationStringParameter(declarationDescriptor, annotation);
    }

    @Nullable
    public static String getNameForAnnotatedObjectWithOverrides(@NotNull DeclarationDescriptor declarationDescriptor) {
        List<DeclarationDescriptor> descriptors;

        if (declarationDescriptor instanceof CallableMemberDescriptor &&
            DescriptorUtils.isOverride((CallableMemberDescriptor) declarationDescriptor)) {

            Set<CallableMemberDescriptor> overriddenDeclarations =
                    DescriptorUtils.getAllOverriddenDeclarations((CallableMemberDescriptor) declarationDescriptor);

            descriptors = ContainerUtil.mapNotNull(overriddenDeclarations, new Function<CallableMemberDescriptor, DeclarationDescriptor>() {
                @Override
                public DeclarationDescriptor fun(CallableMemberDescriptor descriptor) {
                    return DescriptorUtils.isOverride(descriptor) ? null : descriptor;
                }
            });
        }
        else {
            descriptors = ContainerUtil.newArrayList(declarationDescriptor);
        }

        for (DeclarationDescriptor descriptor : descriptors) {
            for (PredefinedAnnotation annotation : PredefinedAnnotation.Companion.getWITH_CUSTOM_NAME()) {
                if (!hasAnnotationOrInsideAnnotatedClass(descriptor, annotation)) {
                    continue;
                }
                String name = getNameForAnnotatedObject(descriptor, annotation);
                return name != null ? name : descriptor.getName().asString();
            }
        }
        return null;
    }

    @Nullable
    private static AnnotationDescriptor getAnnotationByName(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull PredefinedAnnotation annotation
    ) {
        return getAnnotationByName(descriptor, annotation.getFqName());
    }

    @Nullable
    private static AnnotationDescriptor getAnnotationByName(@NotNull DeclarationDescriptor descriptor, @NotNull FqName fqName) {
        return descriptor.getAnnotations().findAnnotation(fqName);
    }

    public static boolean isNativeObject(@NotNull DeclarationDescriptor descriptor) {
        return hasAnnotationOrInsideAnnotatedClass(descriptor, PredefinedAnnotation.NATIVE);
    }

    public static boolean isLibraryObject(@NotNull DeclarationDescriptor descriptor) {
        return hasAnnotationOrInsideAnnotatedClass(descriptor, PredefinedAnnotation.LIBRARY);
    }

    public static boolean isPredefinedObject(@NotNull DeclarationDescriptor descriptor) {
        for (PredefinedAnnotation annotation : PredefinedAnnotation.values()) {
            if (hasAnnotationOrInsideAnnotatedClass(descriptor, annotation)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnnotationOrInsideAnnotatedClass(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull PredefinedAnnotation annotation
    ) {
        return hasAnnotationOrInsideAnnotatedClass(descriptor, annotation.getFqName());
    }

    private static boolean hasAnnotationOrInsideAnnotatedClass(@NotNull DeclarationDescriptor descriptor, @NotNull FqName fqName) {
        if (getAnnotationByName(descriptor, fqName) != null) {
            return true;
        }
        ClassDescriptor containingClass = DescriptorUtils.getContainingClass(descriptor);
        return containingClass != null && hasAnnotationOrInsideAnnotatedClass(containingClass, fqName);
    }

    @Nullable
    public static String getModuleName(@NotNull DeclarationDescriptor declaration) {
        AnnotationDescriptor annotation = declaration.getAnnotations().findAnnotation(JS_MODULE_ANNOTATION);
        return annotation != null ? extractJsModuleName(annotation) : null;
    }

    @Nullable
    public static String getFileModuleName(@NotNull BindingContext bindingContext, @NotNull DeclarationDescriptor declaration) {
        for (AnnotationDescriptor annotation : getContainingFileAnnotations(bindingContext, declaration)) {
            DeclarationDescriptor annotationType = annotation.getType().getConstructor().getDeclarationDescriptor();
            if (annotationType == null) continue;

            FqNameUnsafe fqName = DescriptorUtils.getFqName(annotation.getType().getConstructor().getDeclarationDescriptor());
            if (fqName.equals(JS_MODULE_ANNOTATION.toUnsafe())) {
                return extractJsModuleName(annotation);
            }
        }

        return null;
    }

    public static boolean isNonModule(@NotNull DeclarationDescriptor declaration) {
        return declaration.getAnnotations().findAnnotation(JS_NON_MODULE_ANNOTATION) != null;
    }

    public static boolean isFromNonModuleFile(@NotNull BindingContext bindingContext, @NotNull DeclarationDescriptor declaration) {
        for (AnnotationDescriptor annotation : getContainingFileAnnotations(bindingContext, declaration)) {
            DeclarationDescriptor annotationType = annotation.getType().getConstructor().getDeclarationDescriptor();
            if (annotationType == null) continue;

            FqNameUnsafe fqName = DescriptorUtils.getFqName(annotation.getType().getConstructor().getDeclarationDescriptor());
            if (fqName.equals(JS_NON_MODULE_ANNOTATION.toUnsafe())) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private static String extractJsModuleName(@NotNull AnnotationDescriptor annotation) {
        ConstantValue<?> importValue = annotation.getAllValueArguments().values().iterator().next();
        assert importValue != null : "JsModule annotation should have at least one argument";
        return (String) importValue.getValue();
    }

    @NotNull
    public static List<AnnotationDescriptor> getContainingFileAnnotations(
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor descriptor
    ) {
        PackageFragmentDescriptor containingPackage = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor.class, false);
        if (containingPackage instanceof KotlinJavascriptPackageFragment) {
            return ((KotlinJavascriptPackageFragment) containingPackage).getContainingFileAnnotations(descriptor);
        }

        KtFile kotlinFile = getFile(descriptor);
        if (kotlinFile != null) {
            List<AnnotationDescriptor> annotations = new ArrayList<AnnotationDescriptor>();
            for (KtAnnotationEntry psiAnnotation : kotlinFile.getAnnotationEntries()) {
                AnnotationDescriptor annotation = bindingContext.get(BindingContext.ANNOTATION, psiAnnotation);
                if (annotation != null) {
                    annotations.add(annotation);
                }
            }
            return annotations;
        }

        return Collections.emptyList();
    }

    @Nullable
    private static KtFile getFile(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof DeclarationDescriptorWithSource)) return null;
        SourceFile file = ((DeclarationDescriptorWithSource) descriptor).getSource().getContainingFile();
        if (!(file instanceof PsiSourceFile)) return null;

        PsiFile psiFile = ((PsiSourceFile) file).getPsiFile();
        if (!(psiFile instanceof KtFile)) return null;

        return (KtFile) psiFile;
    }
}

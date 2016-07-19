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

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*

/**
 * Helper for configuring kotlin runtime in tested project.
 */
object ConfigLibraryUtil {
    private val DEFAULT_JAVA_RUNTIME_LIB_NAME = "JAVA_RUNTIME_LIB_NAME"
    private val DEFAULT_KOTLIN_TEST_LIB_NAME = "KOTLIN_TEST_LIB_NAME"
    private val DEFAULT_KOTLIN_JS_STDLIB_NAME = "KOTLIN_JS_STDLIB_NAME"

    fun configureKotlinRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        configureKotlinRuntime(module)
    }

    fun configureKotlinJsRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        configureKotlinJsRuntime(module)
    }

    fun configureKotlinRuntime(module: Module) {
        addLibrary(module, DEFAULT_JAVA_RUNTIME_LIB_NAME) { libModel ->
            libModel.addRoot(VfsUtil.getUrlForLibraryRoot(PathUtil.getKotlinPathsForDistDirectory().runtimePath), OrderRootType.CLASSES)
        }
        addLibrary(module, DEFAULT_KOTLIN_TEST_LIB_NAME) { libModel ->
            libModel.addRoot(VfsUtil.getUrlForLibraryRoot(PathUtil.getKotlinPathsForDistDirectory().kotlinTestPath), OrderRootType.CLASSES)
        }
    }

    fun configureKotlinJsRuntime(module: Module) {
        addLibrary(module, DEFAULT_KOTLIN_JS_STDLIB_NAME) { libModel ->
            libModel.addRoot(VfsUtil.getUrlForLibraryRoot(PathUtil.getKotlinPathsForDistDirectory().jsStdLibJarPath), OrderRootType.CLASSES)
        }
    }

    fun unConfigureKotlinRuntime(module: Module) {
        removeLibrary(module, DEFAULT_JAVA_RUNTIME_LIB_NAME)
        removeLibrary(module, DEFAULT_KOTLIN_TEST_LIB_NAME)
    }

    fun unConfigureKotlinRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        unConfigureKotlinRuntime(module)
    }

    fun unConfigureKotlinJsRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        removeLibrary(module, DEFAULT_KOTLIN_JS_STDLIB_NAME)
    }

    fun configureSdk(module: Module, sdk: Sdk) {
        ApplicationManager.getApplication().runWriteAction {
            val rootManager = ModuleRootManager.getInstance(module)
            val rootModel = rootManager.modifiableModel

            rootModel.sdk = sdk
            rootModel.commit()
        }
    }

    fun addLibrary(module: Module, name: String, callback: (Library.ModifiableModel) -> Unit): Library {
        return ApplicationManager.getApplication().runWriteAction(Computable {
            val rootManager = ModuleRootManager.getInstance(module)
            val model = rootManager.modifiableModel

            val library = model.moduleLibraryTable.createLibrary(name)

            val libModel = library.modifiableModel
            callback(libModel)

            libModel.commit()

            model.commit()

            library
        })
    }

    fun removeLibrary(module: Module, libraryName: String): Boolean {
        return runWriteAction {
            var removed = false

            val rootManager = ModuleRootManager.getInstance(module)
            val model = rootManager.modifiableModel

            for (orderEntry in model.orderEntries) {
                if (orderEntry is LibraryOrderEntry) {

                    val library = orderEntry.library
                    if (library != null) {
                        val name = library.name
                        if (name != null && name == libraryName) {

                            // Dispose attached roots
                            val modifiableModel = library.modifiableModel
                            for (rootUrl in library.rootProvider.getUrls(OrderRootType.CLASSES)) {
                                modifiableModel.removeRoot(rootUrl, OrderRootType.CLASSES)
                            }
                            for (rootUrl in library.rootProvider.getUrls(OrderRootType.SOURCES)) {
                                modifiableModel.removeRoot(rootUrl, OrderRootType.SOURCES)
                            }
                            modifiableModel.commit()

                            model.moduleLibraryTable.removeLibrary(library)

                            removed = true
                            break
                        }
                    }
                }
            }

            model.commit()

            removed
        }
    }

    fun addLibrary(module: Module, libraryName: String, rootPath: String, jarPaths: Array<String>) {
        addLibrary(module, libraryName) { libModel ->
            for (jarPath in jarPaths) {
                libModel.addRoot(VfsUtil.getUrlForLibraryRoot(File(rootPath, jarPath)), OrderRootType.CLASSES)
            }
        }
    }

    fun configureLibraries(module: Module, rootPath: String, libraryInfos: List<String>) {
        for (libraryInfo in libraryInfos) {
            val i = libraryInfo.indexOf('@')
            val libraryName = libraryInfo.substring(0, i)
            val jarPaths = libraryInfo.substring(i + 1).split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            addLibrary(module, libraryName, rootPath, jarPaths)
        }
    }

    fun unconfigureLibrariesByName(module: Module, libraryNames: MutableList<String>) {
        val iterator = libraryNames.iterator()
        while (iterator.hasNext()) {
            val libraryName = iterator.next()
            if (removeLibrary(module, libraryName)) {
                iterator.remove()
            }
        }

        if (!libraryNames.isEmpty()) throw AssertionError("Couldn't find the following libraries: " + libraryNames)
    }

    fun unconfigureLibrariesByInfo(module: Module, libraryInfos: List<String>) {
        val libraryNames = ArrayList<String>()
        for (libraryInfo in libraryInfos) {
            libraryNames.add(libraryInfo.substring(0, libraryInfo.indexOf('@')))
        }
        unconfigureLibrariesByName(module, libraryNames)
    }

    fun configureLibrariesByDirective(module: Module, rootPath: String, fileText: String) {
        configureLibraries(module, rootPath, InTextDirectivesUtils.findListWithPrefixes(fileText, "// CONFIGURE_LIBRARY: "))
    }

    fun unconfigureLibrariesByDirective(module: Module, fileText: String) {
        val libraryNames = ArrayList<String>()
        for (libInfo in InTextDirectivesUtils.findListWithPrefixes(fileText, "// CONFIGURE_LIBRARY: ")) {
            libraryNames.add(libInfo.substring(0, libInfo.indexOf('@')))
        }
        for (libraryName in InTextDirectivesUtils.findListWithPrefixes(fileText, "// UNCONFIGURE_LIBRARY: ")) {
            libraryNames.add(libraryName)
        }

        unconfigureLibrariesByName(module, libraryNames)
    }
}

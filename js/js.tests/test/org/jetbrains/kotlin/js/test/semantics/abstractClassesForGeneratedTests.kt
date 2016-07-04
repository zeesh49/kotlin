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

package org.jetbrains.kotlin.js.test.semantics

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.*
import org.jetbrains.kotlin.serialization.js.ModuleKind

abstract class AbstractBlackBoxTest(d: String) : SingleFileTranslationTest(d) {
    override fun doTest(filename: String) = checkBlackBoxIsOkByPath(filename)
}

// TODO: eliminate when refactoring to single-file annotated tests
abstract class AbstractJsModuleTest(private val moduleKind: ModuleKind, name: String, private val emulateModules: Boolean = true) :
        MultipleFilesTranslationTest("jsModule/$name") {
    override fun setupConfig(configuration: CompilerConfiguration) {
        super.setupConfig(configuration)
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind)
    }

    override fun additionalJsFiles(ecmaVersion: EcmaVersion): MutableList<String> {
        val emulationFiles = if (emulateModules) listOf(BasicTest.MODULE_EMULATION_FILE) else listOf()
        return (emulationFiles + listOf("${pathToTestDir()}/native/${getTestName(true)}.js")).toMutableList()
    }
}

abstract class AbstractAmdModuleTest : AbstractJsModuleTest(ModuleKind.AMD, "amd/")

abstract class AbstractPlainModuleTest : AbstractJsModuleTest(ModuleKind.PLAIN, "plain/")

abstract class AbstractUmdModuleTest : AbstractJsModuleTest(ModuleKind.UMD, "umd/")

abstract class AbstractUmdFallbackModuleTest : AbstractJsModuleTest(ModuleKind.UMD, "umd-fallback/", emulateModules = false)

abstract class AbstractBridgeTest : AbstractBlackBoxTest("bridges/")

abstract class AbstractCallableReferenceTest(main: String) : SingleFileTranslationTest("callableReference/" + main)

abstract class AbstractCompanionObjectTest : SingleFileTranslationTest("objectIntrinsics/")

abstract class AbstractDynamicTest : SingleFileTranslationTest("dynamic/")

abstract class AbstractFunctionExpressionTest : AbstractBlackBoxTest("functionExpression/")

abstract class AbstractInlineEvaluationOrderTest : AbstractSingleFileTranslationWithDirectivesTest("inlineEvaluationOrder/")

abstract class AbstractInlineJsStdlibTest : AbstractSingleFileTranslationWithDirectivesTest("inlineStdlib/")

abstract class AbstractInlineJsTest : AbstractSingleFileTranslationWithDirectivesTest("inline/")

abstract class AbstractJsCodeTest : AbstractSingleFileTranslationWithDirectivesTest("jsCode/")

abstract class AbstractLabelTest : AbstractSingleFileTranslationWithDirectivesTest("labels/")

abstract class AbstractMultiModuleTest : MultipleModulesTranslationTest("multiModule/")

abstract class AbstractInlineMultiModuleTest : MultipleModulesTranslationTest("inlineMultiModule/")

abstract class AbstractReservedWordTest : SingleFileTranslationTest("reservedWords/")

abstract class AbstractSecondaryConstructorTest : AbstractBlackBoxTest("secondaryConstructors/")

abstract class AbstractInnerNestedTest : AbstractBlackBoxTest("innerNested/")

abstract class AbstractClassesTest : AbstractBlackBoxTest("classes/")

abstract class AbstractSuperTest : AbstractBlackBoxTest("super/")

abstract class AbstractLocalClassesTest : AbstractBlackBoxTest("localClasses/")

abstract class AbstractNonLocalReturnsTest : KotlinJSMultiFileTest("inline.generated/nonLocalReturns/")

abstract class AbstractRttiTest : SingleFileTranslationTest("rtti/")

abstract class AbstractCastTest : SingleFileTranslationTest("expression/cast/")

abstract class AbstractLightReflectionTest : SingleFileTranslationTest("reflection/light/")

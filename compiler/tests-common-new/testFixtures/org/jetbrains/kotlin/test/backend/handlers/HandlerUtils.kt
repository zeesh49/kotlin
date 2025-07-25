/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.fir.SequentialPositionFinder
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_ALL_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaInfo
import org.jetbrains.kotlin.test.frontend.fir.handlers.toMetaInfos
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

fun BinaryArtifactHandler<*>.reportKtDiagnostics(module: TestModule, ktDiagnosticReporter: BaseDiagnosticsCollector) {
    if (IGNORE_BACKEND_DIAGNOSTICS in module.directives) {
        return
    }

    val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
    val firParser = module.directives.singleOrZeroValue(FirDiagnosticsDirectives.FIR_PARSER)
    val lightTreeComparingModeEnabled = firParser != null && FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in module.directives
    val lightTreeEnabled = firParser == FirParser.LightTree

    val processedModules = mutableSetOf<TestModule>()
    val diagnosticsService = testServices.diagnosticsService

    fun processModule(module: TestModule) {
        if (!processedModules.add(module)) return
        for (testFile in module.files) {
            val ktDiagnostics = testFile.findByPath(testServices) {
                ktDiagnosticReporter.diagnosticsByFilePath[it]
            } ?: continue
            ktDiagnostics.forEach {
                if (diagnosticsService.shouldRenderDiagnostic(module, it.factoryName, it.severity)) {
                    val metaInfos = it.toMetaInfos(module, testFile, globalMetadataInfoHandler, lightTreeEnabled, lightTreeComparingModeEnabled)
                    globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfos)
                }
            }
        }
        for ((dependantModule, _, _) in module.dependsOnDependencies) {
            processModule(dependantModule)
        }
    }

    processModule(module)
}

fun BinaryArtifactHandler<*>.checkFullDiagnosticRender() {
    val dumper = MultiModuleInfoDumper()
    val moduleStructure = testServices.moduleStructure
    var needToVerifyDiagnostics = false
    for (module in moduleStructure.modules) {
        if (RENDER_ALL_DIAGNOSTICS_FULL_TEXT !in module.directives) continue
        needToVerifyDiagnostics = true
        val reportedDiagnostics = mutableListOf<String>()
        for (testFile in module.files) {
            val finder =
                SequentialPositionFinder(testServices.sourceFileProvider.getContentOfSourceFile(testFile).byteInputStream().reader())
            for (metaInfo in testServices.globalMetadataInfoHandler.getReportedMetaInfosForFile(testFile).sortedBy { it.start }) {
                val rendered = when (metaInfo) {
                    is DiagnosticCodeMetaInfo -> metaInfo.diagnostic.let {
                        val message = DefaultErrorMessages.render(it)
                        val position = DiagnosticUtils.getLineAndColumnRange(it.psiFile, it.textRanges).start
                        renderDiagnosticMessage(it.psiFile.name, it.severity, message, position.line, position.column)
                    }
                    is FirDiagnosticCodeMetaInfo -> metaInfo.diagnostic.let {
                        val message = it.renderMessage()
                        val position = finder.findNextPosition(it.firstRange.startOffset, false)
                        renderDiagnosticMessage(testFile.relativePath, it.severity, message, position.line, position.column)
                    }
                    else -> continue
                }
                reportedDiagnostics += rendered.replace(module.independentSourceDirectoryPath(testServices), "")
            }
        }
        if (reportedDiagnostics.isNotEmpty()) {
            reportedDiagnostics.joinTo(dumper.builderForModule(module), separator = "\n\n", postfix = "\n")
        }
    }

    val expectedFile = File(FileUtil.getNameWithoutExtension(moduleStructure.originalTestDataFiles.first().absolutePath) + ".diag.txt")
    if (needToVerifyDiagnostics) {
        testServices.assertions.assertEqualsToFile(
            expectedFile,
            dumper.generateResultingDump()
        )
    } else {
        val renderAtLeastFrontendDiagnostics = moduleStructure.modules.any { RENDER_DIAGNOSTICS_FULL_TEXT in it.directives }

        if (!renderAtLeastFrontendDiagnostics) {
            testServices.assertions.assertFileDoesntExist(expectedFile, RENDER_ALL_DIAGNOSTICS_FULL_TEXT)
        }
    }
}

private fun renderDiagnosticMessage(fileName: String, severity: Severity, message: String?, line: Int, column: Int): String {
    val severityString = AnalyzerWithCompilerReport.convertSeverity(severity).toString().toLowerCaseAsciiOnly()
    return "/${fileName}:$line:$column: $severityString: $message"
}

fun Assertions.assertFileDoesntExist(file: File, directive: Directive) {
    assertFileDoesntExist(file) { "Dump file detected but no '$directive' directive specified or nothing to dump." }
}

/**
 * Some tests pass relative file names to FirFile/IrFile, and some pass the full path.
 * This utility is intended to cover both of these cases.
 */
fun <R> TestFile.findByPath(testServices: TestServices, finder: (String) -> R?): R? {
    finder("/${this.name}")?.let { return it }
    val realFile = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(this)
    val normalizedPath = FileUtil.toSystemIndependentName(realFile.canonicalPath)
    return finder(normalizedPath)
}

/**
 * Some tests pass relative file names to FirFile/IrFile, and some pass the full path.
 * This utility is intended to cover both of these cases.
 */
fun TestFile.matchPath(testServices: TestServices, matcher: (String) -> Boolean): Boolean {
    return findByPath(testServices) { path -> matcher(path).takeIf { it } } ?: false
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.components.KaCompilerFacility.Companion.CODE_FRAGMENT_CLASS_NAME
import org.jetbrains.kotlin.analysis.api.components.KaCompilerFacility.Companion.CODE_FRAGMENT_METHOD_NAME
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.compilation.CodeFragmentContextDeclarationCache
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaClassBuilderFactory
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaBaseCompiledFileForOutputFile
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaDanglingFileModuleImpl
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleOutputProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedId
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedValueAnalyzer
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerData
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.CompiledCodeProvider
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.CodeFragmentConversionData
import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.resolve.referencedMemberSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedValueParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import org.jetbrains.kotlin.utils.yieldIfNotNull
import java.util.*

/**
 * A source file to be compiled as a part of some [ChunkToCompile].
 */
private class FileToCompile(val ktFile: KtFile, val firFile: FirFile)

/**
 * A set of files to be compiled together.
 *
 * @param kind A chunk kind.
 * @param mainFile The main file if a chunk is the [ChunkKind.MAIN] one, or `null` otherwise.
 * @param hasCodeFragments Whether [files] contain at least one code fragment.
 * @param attachPrecompiledBinaries Whether to attach compiled bytecode of the module instead of compiling the module files.
 * @param files Selected files that are either from the same module or should be compiled as they are from the same module.
 */
private class ChunkToCompile(
    val kind: ChunkKind,
    val mainFile: KtFile?,
    val hasCodeFragments: Boolean,
    val attachPrecompiledBinaries: Boolean,
    val files: List<FileToCompile>
)

private enum class ChunkKind {
    /** A chunk with the main file (a file for which compilation is requested). */
    MAIN,

    /** A chunk with the context of the main file. */
    CONTEXT,

    /**
     * A chunk with files required to be compiled prior to the main file compilation.
     */
    DEPENDENCY
}

private val USE_STDLIB_BUILD_OUTPUT: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Registry.`is`("kotlin.analysis.compilerFacility.useStdlibBuildOutput", true)
}

internal class KaFirCompilerFacility(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaCompilerFacility, KaFirSessionComponent {
    override fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult = withPsiValidityAssertion(file) {
        try {
            return compileUnsafe(file, configuration, target as KaCompilerTarget.Jvm, allowedErrorFilter)
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            throw KaCodeCompilationException(e)
        }
    }

    private fun compileUnsafe(
        mainFile: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget.Jvm,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult {
        val syntaxErrors = SyntaxErrorReportingVisitor(analysisSession.firSession) { it.asKaDiagnostic() }
            .also(mainFile::accept).diagnostics

        if (syntaxErrors.isNotEmpty()) {
            return KaCompilationResult.Failure(syntaxErrors)
        }

        val mainFirFile = getFullyResolvedFirFile(mainFile)

        val codeFragmentMappings = runIf(mainFile is KtCodeFragment) {
            computeCodeFragmentMappings(mainFirFile, resolutionFacade, configuration, target.debuggerExtension)
        }

        val actualizer = LLKindBasedPlatformActualizer(ImplementationPlatformKind.JVM)
        val compilationPeerData = CompilationPeerCollector.process(mainFirFile, actualizer)

        val chunkRegistrar = CompilationChunkRegistrar(mainFile, mainFirFile, target, actualizer)
        val chunks = collectCompilationChunks(chunkRegistrar, compilationPeerData, codeFragmentMappings)

        val jvmIrDeserializer = JvmIrDeserializerImpl()

        val registeredCodeProviders = ArrayList<CompiledCodeProvider>()

        val contextDeclarationCache = runIf(codeFragmentMappings != null) {
            // A code fragment may be moved to a different dangling file module, so here we cannot use the 'mainFile'
            val effectiveCodeFragment = chunks.values.last().mainFile as KtCodeFragment
            val contextDeclaration = effectiveCodeFragment.context?.getNonLocalContainingOrThisDeclaration()
            if (contextDeclaration != null) CodeFragmentContextDeclarationCache(contextDeclaration) else null
        }

        for ((module, chunk) in chunks) {
            ProgressManager.checkCanceled()

            val mainFile = chunk.mainFile
            if (mainFile != null) {
                // Do not check dependency files – even though there might be errors, it's OK as long as they don't affect the main file.
                // This is important for the code evaluation scenario, as people may modify code while debugging.
                // The downside is that we can get unexpected exceptions from the backend (that we wrap into KaCompilationResult.Failure).
                val diagnostics = mainFile.collectDiagnosticsForFile(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                val errors = computeErrors(diagnostics, allowedErrorFilter)
                if (errors.isNotEmpty()) {
                    return KaCompilationResult.Failure(errors)
                }
            }

            if (chunk.attachPrecompiledBinaries) {
                val targetModule = module.baseContextModuleOrSelf as? KaSourceModule
                if (targetModule != null) {
                    val outputDirectory = KotlinModuleOutputProvider.getInstance(project).getCompilationOutput(targetModule)
                    if (outputDirectory != null) {
                        registeredCodeProviders += KaFirDirectoryBasedCompiledCodeProvider(outputDirectory)
                    }
                }

                if (chunk.files.isEmpty()) {
                    continue
                }
            }

            val generateClassFilter = SelectedFilesGenerateClassFilter(
                files = chunk.files.map { it.ktFile },
                inlinedClasses = compilationPeerData.inlinedClasses
            )

            val result = compileChunk(
                module,
                chunk,
                configuration,
                target,
                allowedErrorFilter,
                jvmIrDeserializer,
                codeFragmentMappings?.takeIf { chunk.hasCodeFragments },
                generateClassFilter,
                KaFirDelegatingCompiledCodeProvider(registeredCodeProviders),
                contextDeclarationCache,
            )

            when (result) {
                is KaCompilationResult.Failure -> return result
                is KaCompilationResult.Success if chunk.kind == ChunkKind.MAIN -> return result
                is KaCompilationResult.Success -> {
                    val classMap = buildMap {
                        for (compiledFile in result.output) {
                            val className = getInternalClassName(compiledFile.path) ?: continue

                            // `GenerationState.inlineCache` uses the path to class files without ".class" as a key.
                            // For example,
                            //  - The key for `Foo` class in `com.example.foo` package is `com/example/foo/Foo`.
                            //  - The key for a companion object of `Foo` in `com.example.foo` package is `com/example/foo/Foo$Companion`.
                            //  - The key for an inner class `Inner` of `Foo` in `com.example.foo` package is `com/example/foo/Foo$Inner`.
                            put(className, compiledFile.content)
                        }
                    }

                    registeredCodeProviders += KaFirDependencyCompiledCodeProvider(classMap)
                }
            }
        }

        errorWithAttachment("Unexpectedly skipped the main file") {
            withPsiEntry("file", mainFile)
        }
    }

    private fun getInternalClassName(classFilePath: String): String? {
        if (classFilePath.endsWith(".class", ignoreCase = true)) {
            return classFilePath.dropLast(".class".length)
        }
        return null
    }

    private fun collectCompilationChunks(
        chunkRegistrar: CompilationChunkRegistrar,
        compilationPeerData: CompilationPeerData,
        codeFragmentMappings: CodeFragmentMappings?
    ): Map<KaModule, ChunkToCompile> {
        for ((module, files) in compilationPeerData.peers) {
            for (file in files) {
                chunkRegistrar.submit(file, module)
            }
        }

        if (codeFragmentMappings != null) {
            for (capturedFile in codeFragmentMappings.capturedFiles) {
                val module = resolutionFacade.getModule(capturedFile)
                chunkRegistrar.submit(capturedFile, module)
            }
        }

        return chunkRegistrar.computeChunks()
    }

    /**
     * Configuration of a compilation chunk to be created.
     *
     * @param effectiveModule The module to which all files in the chunk either belong or have as a context.
     * @param kind Whether the chunk contains the main file (a file for which compilation was requested), its context or a dependency.
     * @param isDanglingChild Whether a new dangling module with the [effectiveModule] as a context module must be created, instead of reusing
     *   [effectiveModule] where possible.
     * @param attachPrecompiledBinaries Whether to attach compiled bytecode of the module instead of compiling the module files.
     */
    private data class ChunkSpec(
        val originalModule: KaModule,
        val effectiveModule: KaModule,
        val kind: ChunkKind,
        val isDanglingChild: Boolean,
        val attachPrecompiledBinaries: Boolean,
    )

    /**
     * A facility for splitting the list of input files into chunks.
     *
     * Here is how to use it:
     * 1. Call [submit], passing every file to compile (including the dependencies).
     * 2. Call [computeChunks] to get the resulting map of chunks.
     *
     * @param originalMainFile The unmodified [KtFile] for the main file.
     * @param originalMainFirFile The [FirFile] representing the [originalMainFile].
     * @param target The compilation target.
     */
    private inner class CompilationChunkRegistrar(
        private val originalMainFile: KtFile,
        private val originalMainFirFile: FirFile,
        private val target: KaCompilerTarget,
        private val actualizer: LLPlatformActualizer?
    ) {
        private val originalMainModule = originalMainFirFile.llFirModuleData.ktModule
        private val originalMainContextModule = (originalMainModule as? KaDanglingFileModule)?.contextModule

        private val submittedChunks = LinkedHashMap<ChunkSpec, MutableSet<KtFile>>()

        /**
         * Attach the file to the appropriate chunk.
         * The [module] parameter is used for optimization, and it corresponds to [LLResolutionFacade.getModule] called on the [file].
         */
        fun submit(file: KtFile, module: KaModule) {
            val chunkKind = when (module) {
                originalMainModule -> ChunkKind.MAIN
                originalMainContextModule -> ChunkKind.CONTEXT
                else -> ChunkKind.DEPENDENCY
            }

            val attachPrecompiledBinaries = shouldAttachPrecompiledBinaries(file)

            fun register(spec: ChunkSpec, file: KtFile, alwaysAttachFile: Boolean = false) {
                val chunkSpec = submittedChunks.getOrPut(spec, ::LinkedHashSet)

                // Even if precompiled binaries should be attached instead of source files, still attach dangling files.
                // Covered scenario: debugging the 'kotlin-stdlib' implementation.
                if (alwaysAttachFile || !attachPrecompiledBinaries) {
                    chunkSpec.add(file)
                }
            }

            when {
                module is KaDanglingFileModule -> {
                    if (module.isSupported) {
                        val spec = ChunkSpec(
                            originalModule = module,
                            effectiveModule = module,
                            kind = chunkKind,
                            isDanglingChild = false,
                            attachPrecompiledBinaries = attachPrecompiledBinaries
                        )
                        register(spec, file, alwaysAttachFile = true)
                    } else {
                        val spec = ChunkSpec(
                            originalModule = module,
                            effectiveModule = substitute(module.contextModule),
                            kind = chunkKind,
                            isDanglingChild = true,
                            attachPrecompiledBinaries = attachPrecompiledBinaries
                        )
                        register(spec, file, alwaysAttachFile = true)
                    }
                }

                module.isSupported && chunkKind != ChunkKind.MAIN -> {
                    val spec = ChunkSpec(
                        originalModule = module,
                        effectiveModule = module,
                        kind = chunkKind,
                        isDanglingChild = false,
                        attachPrecompiledBinaries = attachPrecompiledBinaries
                    )
                    register(spec, file)
                }

                else -> {
                    val substitutedModule = substitute(module)
                    // If the module is still not a JVM one, we have to create a dangling file module for it, as the JVM compiler
                    // cannot compile files from common modules. More precisely, it can consume files with 'expect' declarations,
                    // but dependencies of common modules are also common. From those, the backend cannot get the required information
                    // (JVM facade class names, bytecode for inlining, and so on).
                    val spec = ChunkSpec(
                        originalModule = module,
                        effectiveModule = substitutedModule,
                        kind = chunkKind,
                        isDanglingChild = module != substitutedModule || !module.targetPlatform.isJvm(),
                        attachPrecompiledBinaries = attachPrecompiledBinaries
                    )
                    register(spec, file)
                }
            }
        }

        private fun shouldAttachPrecompiledBinaries(file: KtFile): Boolean {
            if (file is KtCodeFragment) {
                val contextFile = file.context?.containingFile
                return if (contextFile is KtFile) shouldAttachPrecompiledBinaries(contextFile) else false
            }

            // Support for debugging in the Kotlin compiler repository.
            // There, the 'kotlin-stdlib' comes as a source, and compiling it is non-trivial and inefficient.
            return USE_STDLIB_BUILD_OUTPUT && file.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
        }

        /**
         * Whether the [KaFirCompilerFacility] supports compilation of the given module.
         * Currently, only JVM modules are supported.
         */
        private val KaModule.isSupported: Boolean
            get() = when (target) {
                is KaCompilerTarget.Jvm -> targetPlatform.isJvm()
            }

        private val KaModule.implementingJvmModule: KaModule?
            get() = actualizer?.actualize(this)

        private val moduleCache = HashMap<KaModule, KaModule>()

        private fun substitute(module: KaModule): KaModule {
            require(module !is KaDanglingFileModule) { "Compilation of nested dangling file modules is not supported" }

            return moduleCache.computeIfAbsent(module) { module ->
                if (module.targetPlatform.isCommon() && target is KaCompilerTarget.Jvm) {
                    module.implementingJvmModule ?: module
                } else {
                    module
                }
            }
        }

        /**
         * Compute chunks containing all files passed to [submit].
         *
         * The main chunk is guaranteed to be the last one in the returned [Map].
         * Other chunks generally follow the order of file submission.
         */
        fun computeChunks(): Map<KaModule, ChunkToCompile> {
            val (mainChunks, otherChunks) = submittedChunks.entries.partition { it.key.kind == ChunkKind.MAIN }
            val result = LinkedHashMap<KaModule, ChunkToCompile>()

            // Contains mappings from original modules to the modules that should be used instead.
            // The mappings are used for substituting context modules of the dependent dangling file chunks.
            // E.g., if 'common' is substituted with 'jvm', a code fragment with the 'common' context module
            // becomes a new dangling file module with the 'jvm' context.
            val moduleSubstitutions = HashMap<KaModule, KaModule>()

            // Cross-module file substitutions.
            // Used for patching context of code fragments.
            val fileSubstitutions = HashMap<KtFile, KtFile>()

            tailrec fun mapModule(originalModule: KaModule): KaModule? {
                if (originalModule is KaDanglingFileModule) {
                    return mapModule(originalModule.contextModule)
                }
                return moduleSubstitutions[originalModule]
            }

            /**
             * Create a new multi-file dangling file module, containing copies of [files], with the specified [contextModule].
             */
            fun appendDanglingChunk(spec: ChunkSpec, files: List<KtFile>) {
                val newFiles = buildList(files.size) {
                    for (file in files) {
                        val fileCopy = createFileCopy(file, fileSubstitutions)
                        fileSubstitutions[file] = fileCopy
                        add(fileCopy)
                    }
                }

                val mainFile = if (spec.kind == ChunkKind.MAIN) {
                    val mainFileIndex = files.indexOf(originalMainFile)
                    check(mainFileIndex >= 0) { "Main file is not submitted" }
                    newFiles[mainFileIndex]
                } else {
                    null
                }

                val contextModule = mapModule(spec.originalModule) ?: spec.effectiveModule
                val newModule = createDanglingFileModule(newFiles, contextModule)
                newFiles.forEach { it.explicitModule = newModule }

                moduleSubstitutions[spec.originalModule] = newModule

                result[newModule] = ChunkToCompile(
                    spec.kind,
                    mainFile = mainFile,
                    hasCodeFragments = newFiles.any { it is KtCodeFragment },
                    attachPrecompiledBinaries = spec.attachPrecompiledBinaries,
                    files = createFilesToCompile(newFiles)
                )
            }

            fun process(entries: List<Map.Entry<ChunkSpec, Set<KtFile>>>) {
                for ((spec, files) in entries) {
                    if (spec.isDanglingChild) {
                        // Creation of the new dangling file module is explicitly requested.
                        appendDanglingChunk(spec, files.toList())
                    } else {
                        val hasCodeFragments = files.any { it is KtCodeFragment }

                        val mainFile = if (spec.kind == ChunkKind.MAIN) {
                            check(originalMainFile in files) { "Main file is not submitted" }
                            originalMainFile
                        } else {
                            null
                        }

                        moduleSubstitutions[spec.originalModule] = spec.effectiveModule

                        result[spec.effectiveModule] = ChunkToCompile(
                            kind = spec.kind,
                            mainFile = mainFile,
                            hasCodeFragments = hasCodeFragments,
                            attachPrecompiledBinaries = spec.attachPrecompiledBinaries,
                            files = createFilesToCompile(files)
                        )
                    }
                }
            }

            // The main chunk needs to be the last one
            process(otherChunks)
            process(mainChunks)

            return result
        }

        fun createDanglingFileModule(newFiles: List<KtFile>, contextModule: KaModule): KaDanglingFileModule {
            val baseModule = KaDanglingFileModuleImpl(newFiles, contextModule, KaDanglingFileResolutionMode.PREFER_SELF)

            if (!contextModule.targetPlatform.isJvm()) {
                return createJvmDanglingFileModule(baseModule, contextModule)
            }

            return baseModule
        }

        /**
         * Create a custom dangling file module with the substituted target platform and dependencies.
         *
         * For multiplatform modules with common and implementation parts, we can use the JVM implementation module for compiling
         * common declarations. However, in cases when there is no JVM module at all, we have to create one.
         */
        private fun createJvmDanglingFileModule(baseModule: KaDanglingFileModule, contextModule: KaModule): KaDanglingFileModule {
            val dependentSourceModules = KotlinModuleDependentsProvider.getInstance(project)
                .getTransitiveDependents(contextModule)
                .asSequence()
                .filterIsInstance<KaSourceModule>()
                .filter { it.targetPlatform.isJvm() }
                .sortedBy { it.name }
                .toList()

            val minimalJvmTarget = dependentSourceModules
                .asSequence()
                .flatMap { it.targetPlatform.componentPlatforms }
                .distinct()
                .mapNotNull { it.targetPlatformVersion as? JvmTarget }
                .sortedBy { it.majorVersion }
                .firstOrNull()

            // Check in which JVM modules the context module is used and pick the lowest JVM target,
            // so it is compatible with all usages.
            val minimalTargetPlatform = minimalJvmTarget?.let(JvmPlatforms::jvmPlatformByTargetVersion)
                ?: JvmPlatforms.defaultJvmPlatform

            val newBinaryDependencies = mergeJvmImplementationBinaryDependencies(dependentSourceModules)
            val newSourceDependencies = collectJvmImplementationSourceDependencies(contextModule)

            val newDependencies = sequenceOf(newBinaryDependencies, newSourceDependencies)
                .flatten()
                .toList()

            return object : KaDanglingFileModule by baseModule {
                override val targetPlatform: TargetPlatform
                    get() = minimalTargetPlatform

                override val directRegularDependencies: List<KaModule>
                    get() = newDependencies
            }
        }

        /**
         * Collect JVM counterparts of source modules on which the [contextModule] depends.
         * These would be (at least some of the) dependencies of the JVM counterpart of the [contextModule] itself.
         */
        private fun collectJvmImplementationSourceDependencies(contextModule: KaModule): Sequence<KaSourceModule> {
            return sequence {
                val processedModules = HashSet<KaModule>()

                val pendingModules = ArrayDeque<KaModule>()
                pendingModules.addLast(contextModule)

                while (pendingModules.isNotEmpty()) {
                    val module = pendingModules.removeLast()

                    for (dependency in module.directRegularDependencies) {
                        if (dependency is KaSourceModule && processedModules.add(dependency)) {
                            val dependencyImplementing = dependency.implementingJvmModule as? KaSourceModule
                            val dependencyEffective = dependencyImplementing ?: dependency

                            // Check for duplicates once more – we could get the implementing module through JVM dependencies
                            // and also through the common module implementation
                            if (dependencyImplementing == null || processedModules.add(dependencyEffective)) {
                                yield(dependencyEffective)

                                // Also, add source dependencies of the implementing module.
                                // Note that this adds dependencies to modules unavailable in the original context.
                                // This may lead to a classpath hell – however, not adding the dependencies can also lead to
                                // failure in code inlining.
                                pendingModules.addLast(dependencyEffective)
                            }
                        }
                    }
                }
            }
        }

        /**
         * Collect common JVM binary module dependencies between all provided [dependentSourceModules].
         * The goal is to figure out what dependencies should be provided instead of the dependency to a common module.
         */
        private fun mergeJvmImplementationBinaryDependencies(dependentSourceModules: List<KaSourceModule>): Sequence<KaLibraryModule> {
            return sequence {
                val dependentModuleDependencies = dependentSourceModules
                    .map { collectJvmImplementationBinaryDependencies(it) }

                var sdkModule: KaLibraryModule? = null

                val commonBinaryDependencies = LinkedHashSet<KaLibraryModule>()
                var isFirst = true

                for (dependencySequence in dependentModuleDependencies) {
                    val dependencies = dependencySequence.toList()

                    if (sdkModule == null) {
                        // Pick the first SDK for now.
                        // In the scope of KT-77426, this should become customizable.
                        sdkModule = dependencies.firstOrNull { it.isSdk }
                    }

                    if (isFirst) {
                        commonBinaryDependencies.addAll(dependencies)
                        isFirst = false
                    } else {
                        commonBinaryDependencies.retainAll(dependencies)
                    }
                }

                if (sdkModule != null) {
                    commonBinaryDependencies.remove(sdkModule)
                }

                yieldIfNotNull(sdkModule)
                yieldAll(commonBinaryDependencies)
            }
        }

        /**
         * Collect JVM binary module dependencies of the given [contextModule], including transitive dependencies.
         */
        private fun collectJvmImplementationBinaryDependencies(contextModule: KaModule): Sequence<KaLibraryModule> {
            return sequence {
                val processedModules = HashSet<KaModule>()

                val pendingModules = ArrayDeque<KaModule>()
                pendingModules.addLast(contextModule)

                while (pendingModules.isNotEmpty()) {
                    val module = pendingModules.removeLast()
                    for (dependency in module.directRegularDependencies) {
                        if (!processedModules.add(dependency)) {
                            continue
                        }

                        if (dependency is KaLibraryModule) {
                            if (dependency.targetPlatform.isJvm()) {
                                yield(dependency)
                            }
                        } else {
                            pendingModules.addLast(dependency)
                        }
                    }
                }
            }
        }

        private fun createFilesToCompile(files: Collection<KtFile>): List<FileToCompile> {
            return buildList {
                for (file in files) {
                    val firFile = if (file == originalMainFile) originalMainFirFile else getFullyResolvedFirFile(file)
                    add(FileToCompile(file, firFile))
                }

                // Code fragments must go after all context files so the backend works correctly
                sortBy { it.ktFile is KtCodeFragment }
            }
        }

        /**
         * Creates a copy of the [file].
         *
         * For code fragments, the context is substituted using [fileSubstitutions].
         * File content in substitution key-value pairs must be identical.
         */
        private fun createFileCopy(file: KtFile, fileSubstitutions: Map<KtFile, KtFile>): KtFile {
            val newName = file.name
            val newText = file.text

            if (file is KtCodeFragment) {
                val newContext = substituteContext(file.context, fileSubstitutions)

                return when (file) {
                    is KtExpressionCodeFragment -> KtExpressionCodeFragment(project, newName, newText, file.importsToString(), newContext)
                    is KtBlockCodeFragment -> KtBlockCodeFragment(project, newName, newText, file.importsToString(), newContext)
                    is KtTypeCodeFragment -> KtTypeCodeFragment(project, newName, newText, newContext)
                    else -> error("Unsupported code fragment type: " + file.javaClass.name)
                }
            }

            return KtPsiFactory(project).createPhysicalFile(newName, newText)
        }

        private fun substituteContext(context: PsiElement?, fileSubstitutions: Map<KtFile, KtFile>): PsiElement? {
            val containingFile = context?.containingFile ?: return context
            val substitutedFile = fileSubstitutions[containingFile] ?: return context
            return PsiTreeUtil.findSameElementInCopy(context, substitutedFile)
        }
    }

    private fun runJvmIrCodeGen(
        chunk: ChunkToCompile,
        fir2IrResult: Fir2IrActualizedResult,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget.Jvm,
        codeFragmentMappings: CodeFragmentMappings?,
        codegenFactory: JvmIrCodegenFactory,
        generateClassFilter: GenerationState.GenerateClassFilter,
        diagnosticReporter: PendingDiagnosticsCollectorWithSuppress,
        jvmGeneratorExtensions: JvmGeneratorExtensions,
        compiledCodeProvider: CompiledCodeProvider,
    ): KaCompilationResult {
        val matchingClassNames = mutableSetOf<String>()

        val classBuilderFactory = KaClassBuilderFactory.create(
            delegateFactory = if (target.isTestMode) ClassBuilderFactories.TEST else ClassBuilderFactories.BINARIES,
            compiledClassHandler = { file, className ->
                target.compiledClassHandler?.handleClassDefinition(file, className)

                // Synthetic classes often don't have a source element attached, so judging whether the class should stay is hard
                if (chunk.mainFile == null || file == chunk.mainFile) {
                    // If any nested class matches the filter, the outer class should also match it.
                    // We can meet false positives here: it's OK as it's better than to filter out a required class by a mistake.
                    val topLevelClassName = className.substringBefore('$')
                    matchingClassNames.add(topLevelClassName)
                }
            }
        )

        val generationState = GenerationState(
            project,
            fir2IrResult.irModuleFragment.descriptor,
            configuration,
            classBuilderFactory,
            generateDeclaredClassFilter = generateClassFilter,
            diagnosticReporter = diagnosticReporter,
            compiledCodeProvider = compiledCodeProvider
        )

        ProgressManager.checkCanceled()

        val backendInput = JvmIrCodegenFactory.BackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.pluginContext.irBuiltIns,
            fir2IrResult.symbolTable,
            fir2IrResult.components.irProviders,
            CompilerFacilityJvmGeneratorExtensions(jvmGeneratorExtensions),
            FirJvmBackendExtension(fir2IrResult.components, null),
            fir2IrResult.pluginContext,
        )
        codegenFactory.generateModule(generationState, backendInput)

        fun isMatchingRelativeClassPath(path: String): Boolean {
            val className = path.takeIf { it.endsWith(".class", ignoreCase = true) }?.dropLast(".class".length) ?: return false
            return className in matchingClassNames || matchingClassNames.any { className.startsWith("$it$") }
        }

        // Generate class filter makes the backend skip class bodies, but classes are still generated.
        // Here we filter the unnecessary output.
        val compiledFiles = generationState.factory.asList()
            .filter { isMatchingRelativeClassPath(it.relativePath) }
            .map(::KaBaseCompiledFileForOutputFile)

        val capturedValues = buildList {
            if (codeFragmentMappings != null) {
                addAll(codeFragmentMappings.capturedValues)
            }
            for ((_, _, descriptor) in generationState.newFragmentCaptureParameters) {
                if (descriptor is IrBasedDeclarationDescriptor<*>) {
                    addIfNotNull(computeAdditionalCodeFragmentMapping(descriptor))
                }
            }
        }

        require(compiledFiles.isNotEmpty()) { "Compilation produced no matching output files" }
        return KaCompilationResult.Success(
            compiledFiles,
            capturedValues,
            canBeCached = codeFragmentMappings?.reifiedTypeParametersMapping.isNullOrEmpty()
        )
    }

    private fun getIrGenerationExtensions(module: KaModule): List<IrGenerationExtension> {
        val projectExtensions = IrGenerationExtension.getInstances(project)

        val unwrappedModule = module.baseContextModuleOrSelf
        if (unwrappedModule !is KaSourceModule) {
            return projectExtensions
        }

        val moduleExtensions = KotlinCompilerPluginsProvider.getInstance(project)
            ?.getRegisteredExtensions(unwrappedModule, IrGenerationExtension)
            .orEmpty()

        return moduleExtensions + projectExtensions
    }

    private fun compileChunk(
        module: KaModule,
        chunk: ChunkToCompile,
        baseConfiguration: CompilerConfiguration,
        target: KaCompilerTarget.Jvm,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
        jvmIrDeserializer: JvmIrDeserializer,
        codeFragmentMappings: CodeFragmentMappings?,
        generateClassFilter: GenerationState.GenerateClassFilter,
        compiledCodeProvider: CompiledCodeProvider,
        contextDeclarationCache: CodeFragmentContextDeclarationCache?,
    ): KaCompilationResult {
        val session = resolutionFacade.sessionProvider.getResolvableSession(module)
        val configuration = baseConfiguration.copy().apply {
            put(CommonConfigurationKeys.USE_FIR, true)
            put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, session.languageVersionSettings)
        }

        val baseFir2IrExtensions = JvmFir2IrExtensions(configuration, jvmIrDeserializer)

        val fir2IrExtensions = when {
            codeFragmentMappings != null && chunk.mainFile != null -> {
                val injectedValueProvider = InjectedSymbolProvider(codeFragmentMappings, chunk.mainFile)
                CodeFragmentFir2IrExtensions(baseFir2IrExtensions, injectedValueProvider)
            }
            chunk.kind == ChunkKind.CONTEXT -> {
                CodeFragmentContextFir2IrExtensions(baseFir2IrExtensions, contextDeclarationCache)
            }
            else -> baseFir2IrExtensions
        }

        val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector)

        val commonMemberStorage = contextDeclarationCache?.customCommonMemberStorage ?: Fir2IrCommonMemberStorage()

        val fir2IrResult = runFir2Ir(
            session = session,
            firFiles = chunk.files.map { it.firFile },
            fir2IrExtensions = fir2IrExtensions,
            diagnosticReporter = diagnosticReporter,
            effectiveConfiguration = configuration,
            irGeneratorExtensions = getIrGenerationExtensions(module),
            commonMemberStorage = commonMemberStorage
        )

        val convertedMapping = codeFragmentMappings?.reifiedTypeParametersMapping.orEmpty().entries.associate { (firTypeParam, coneType) ->
            val irTypeParam = fir2IrResult.components.classifierStorage.getIrTypeParameterSymbol(firTypeParam, ConversionTypeOrigin.DEFAULT)
            irTypeParam to with(fir2IrResult.components) { coneType.toIrType() }
        }

        if (diagnosticReporter.hasErrors) {
            val errors = computeErrors(diagnosticReporter.diagnostics.filterIsInstance<KtDiagnosticWithSource>(), allowedErrorFilter)
            if (errors.isNotEmpty()) {
                return KaCompilationResult.Failure(errors)
            }
        }

        val evaluatorData = when (chunk.kind) {
            ChunkKind.MAIN if codeFragmentMappings != null -> {
                val irFile = fir2IrResult.irModuleFragment.files.single { (it.fileEntry as? PsiIrFileEntry)?.psiFile is KtCodeFragment }
                val irClass = irFile.declarations.single { it is IrClass && it.metadata is FirMetadataSource.CodeFragment } as IrClass
                val irFunction = irClass.declarations.single { it is IrFunction && it !is IrConstructor } as IrFunction

                val localDeclarationsData = contextDeclarationCache?.localDeclarationsData
                    ?: JvmBackendContext.SharedLocalDeclarationsData()

                JvmEvaluatorData(
                    localDeclarationsData = localDeclarationsData,
                    evaluatorGeneratedFunction = irFunction,
                    capturedTypeParametersMapping = convertedMapping
                )
            }
            ChunkKind.CONTEXT -> {
                JvmEvaluatorData(
                    localDeclarationsData = JvmBackendContext.SharedLocalDeclarationsData(),
                    evaluatorGeneratedFunction = null,
                    capturedTypeParametersMapping = convertedMapping
                )
            }
            else -> null
        }

        val codegenFactory = createJvmIrCodegenFactory(configuration, evaluatorData)

        val result = runJvmIrCodeGen(
            chunk,
            fir2IrResult,
            configuration,
            target,
            codeFragmentMappings,
            codegenFactory,
            generateClassFilter,
            diagnosticReporter,
            baseFir2IrExtensions,
            compiledCodeProvider
        )

        if (diagnosticReporter.hasErrors) {
            val errors = computeErrors(diagnosticReporter.diagnostics.filterIsInstance<KtDiagnosticWithSource>(), allowedErrorFilter)
            if (errors.isNotEmpty()) {
                return KaCompilationResult.Failure(errors)
            }
        }

        if (chunk.kind == ChunkKind.CONTEXT) {
            contextDeclarationCache?.initialize(commonMemberStorage, evaluatorData)
        }

        return result
    }

    private fun runFir2Ir(
        session: LLFirSession,
        firFiles: List<FirFile>,
        fir2IrExtensions: Fir2IrExtensions,
        diagnosticReporter: BaseDiagnosticsCollector,
        effectiveConfiguration: CompilerConfiguration,
        irGeneratorExtensions: List<IrGenerationExtension>,
        commonMemberStorage: Fir2IrCommonMemberStorage
    ): Fir2IrActualizedResult {
        val fir2IrConfiguration =
            Fir2IrConfiguration.forAnalysisApi(effectiveConfiguration, session.languageVersionSettings, diagnosticReporter)
        val firResult = FirResult(listOf(ModuleCompilerAnalyzedOutput(session, session.getScopeSession(), firFiles)))
        val singleOutput = firResult.outputs.size == 1
        check(singleOutput) { "Single output invariant is used in the lambda below" }

        return firResult.convertToIrAndActualize(
            fir2IrExtensions = fir2IrExtensions,
            fir2IrConfiguration = fir2IrConfiguration,
            irGeneratorExtensions = irGeneratorExtensions,
            irMangler = JvmIrMangler,
            visibilityConverter = FirJvmVisibilityConverter,
            kotlinBuiltIns = DefaultBuiltIns.Instance,
            typeSystemContextProvider = ::JvmIrTypeSystemContext,
            specialAnnotationsProvider = JvmIrSpecialAnnotationSymbolProvider,
            extraActualDeclarationExtractorsInitializer = {
                error(
                    "extraActualDeclarationExtractorsInitializer should never be called, because outputs is a list of a single element. " +
                            "Output is single => " +
                            "dependentIrFragments will always be empty => " +
                            "IrActualizer will never be called => " +
                            "extraActualDeclarationExtractorsInitializer will never be called"
                )
            },
            commonMemberStorage = commonMemberStorage
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun computeAdditionalCodeFragmentMapping(descriptor: IrBasedDeclarationDescriptor<*>): CodeFragmentCapturedValue? {
        val owner = descriptor.owner

        if (descriptor is IrBasedReceiverParameterDescriptor && owner is IrValueParameter) {
            val receiverClass = (owner.type as? IrSimpleType)?.classifier as? IrClassSymbol
            val receiverClassId = receiverClass?.owner?.classId

            val parent = owner.parent
            if (receiverClassId != null && parent is IrFunction) {
                when (owner.kind) {
                    IrParameterKind.DispatchReceiver -> {
                        return CodeFragmentCapturedValue.ContainingClass(receiverClassId, isCrossingInlineBounds = true)
                    }
                    IrParameterKind.Context -> {
                        val contextParameterIndex = parent.parameters
                            .subList(0, owner.indexInParameters)
                            .count { it.kind == IrParameterKind.Context }
                        val labelName = receiverClassId.shortClassName
                        return CodeFragmentCapturedValue.ContextReceiver(contextParameterIndex, labelName, isCrossingInlineBounds = true)
                    }
                    IrParameterKind.ExtensionReceiver -> {
                        return CodeFragmentCapturedValue.ExtensionReceiver(parent.name.asString(), isCrossingInlineBounds = true)
                    }
                    IrParameterKind.Regular -> {}
                }
            }
        }

        if (descriptor is IrBasedVariableDescriptor && owner is IrVariable) {
            val name = owner.name
            val isMutated = false // TODO capture the usage somehow

            if (owner.origin == IrDeclarationOrigin.PROPERTY_DELEGATE) {
                return CodeFragmentCapturedValue.LocalDelegate(name, isMutated, isCrossingInlineBounds = true)
            }

            return CodeFragmentCapturedValue.Local(name, isMutated, isCrossingInlineBounds = true)
        }

        if (descriptor is IrBasedValueParameterDescriptor && owner is IrValueParameter) {
            val name = owner.name
            return CodeFragmentCapturedValue.Local(name, isMutated = false, isCrossingInlineBounds = true)
        }

        return null
    }

    private fun getFullyResolvedFirFile(file: KtFile): FirFile {
        val firFile = file.getOrBuildFirFile(resolutionFacade)
        firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
        return firFile
    }

    private fun computeErrors(
        diagnostics: Collection<DiagnosticMarker>,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
    ): List<KaDiagnostic> {
        return buildList {
            for (diagnostic in diagnostics) {
                require(diagnostic is KtDiagnostic)
                if (diagnostic.severity == Severity.ERROR) {
                    val ktDiagnostic = when (diagnostic) {
                        is KtPsiDiagnostic -> diagnostic.asKaDiagnostic()
                        else -> {
                            val message = diagnostic.renderMessage()
                            KaNonBoundToPsiErrorDiagnostic(diagnostic.factoryName, message, analysisSession.token)
                        }
                    }
                    if (!allowedErrorFilter(ktDiagnostic)) {
                        add(ktDiagnostic)
                    }
                }
            }
        }
    }

    private class CodeFragmentMappings(
        val capturedValues: List<CodeFragmentCapturedValue>,
        val capturedFiles: List<KtFile>,
        val injectedValues: List<InjectedValue>,
        val conversionData: CodeFragmentConversionData,
        val reifiedTypeParametersMapping: Map<FirTypeParameterSymbol, ConeKotlinType>,
    )

    @OptIn(LLFirInternals::class)
    private fun computeCodeFragmentMappings(
        mainFirFile: FirFile,
        resolutionFacade: LLResolutionFacade,
        configuration: CompilerConfiguration,
        debuggerExtension: DebuggerExtension?,
    ): CodeFragmentMappings {
        val codeFragment = mainFirFile.codeFragment

        val capturedData = CodeFragmentCapturedValueAnalyzer.analyze(resolutionFacade, codeFragment)

        val capturedSymbols = capturedData.symbols
        val capturedValues = capturedSymbols.map { it.value }
        val injectedValues = capturedSymbols.map { InjectedValue(it.symbol, it.typeRef, it.value.isMutated) }

        val conversionData = CodeFragmentConversionData(
            classId = ClassId(FqName.ROOT, Name.identifier(configuration[CODE_FRAGMENT_CLASS_NAME] ?: "CodeFragment")),
            methodName = Name.identifier(configuration[CODE_FRAGMENT_METHOD_NAME] ?: "run"),
            injectedValues
        )

        val capturedReifiedTypeParametersMap =
            collectReifiedTypeParametersMapping(capturedData.reifiedTypeParameters, debuggerExtension).toMutableMap()

        val typeSubstitutor = substitutorByMap(capturedReifiedTypeParametersMap, resolutionFacade.useSiteFirSession)

        // The parameters are ordered in the map according to the order of declaring function in execution stack, e.g.:
        //
        // fun <reified T3> foo3() {
        //     ...suspension point...
        // }
        // fun <reified T2> foo2() {
        //     foo3<T2>()
        // }
        // fun <reified T1> foo1() {
        //     foo2<T1>()
        // }
        // ... entry point...
        // fun main() {
        //     foo1<Int>()
        // }
        //
        // Parameters will be ordered as T3, T2, T1, i.e. argument follows the parameter.
        // Thus, processing them in reversive order gives the transitive closure of substitution.
        for (typeParameter in capturedReifiedTypeParametersMap.keys.reversed().iterator()) {
            capturedReifiedTypeParametersMap[typeParameter] =
                typeSubstitutor.substituteOrSelf(capturedReifiedTypeParametersMap[typeParameter]!!)
        }

        return CodeFragmentMappings(
            capturedValues,
            capturedData.files,
            injectedValues,
            conversionData,
            // It's vital to leave only parameters immediately captured by code fragment, as JVM ReifiedTypeInliner does not distinguish
            // different type parameters with the same name
            // See IntelliJ test:
            // community/plugins/kotlin/jvm-debugger/test/testData/evaluation/singleBreakpoint/reifiedTypeParameters/crossfileInlining.kt
            capturedReifiedTypeParametersMap.filterKeys { it in capturedData.reifiedTypeParameters })
    }

    private fun collectReifiedTypeParametersMapping(
        capturedReifiedTypeParameters: Set<FirTypeParameterSymbol>,
        debuggerExtension: DebuggerExtension?,
    ): Map<FirTypeParameterSymbol, ConeKotlinType> {
        fun ConeKotlinType.collectTypeParameters(destination: MutableSet<FirTypeParameterSymbol>) {
            if (this is ConeTypeParameterType) {
                destination.add(lookupTag.typeParameterSymbol)
                return
            }
            typeArguments.forEach { typeArgument ->
                typeArgument.type?.collectTypeParameters(destination)
            }
        }

        fun FirTypeRef.collectTypeParameters(destination: MutableSet<FirTypeParameterSymbol>) =
            (this as? FirResolvedTypeRef)?.coneType?.collectTypeParameters(destination)

        // We need to save the order to make a substitution in the correct order later
        val mapping = linkedMapOf<FirTypeParameterSymbol, FirTypeRef>()
        if (debuggerExtension == null) return linkedMapOf()
        val unmappedTypeParameters = capturedReifiedTypeParameters.toMutableSet()

        // We roll back along the execution stack, until either all required type parameters are mapped on arguments, or
        // we are unable to proceed further.
        // E.g., we might reach the execution stack beginning or fail to extract relevant info from the call.
        // There are cases when a code fragment captures a reified type parameter, but we are still able to compile it
        // without reification, that is why we avoid fast-failing here when not all the type parameters are mapped.
        val stackIterator = debuggerExtension.stack.iterator()
        while (unmappedTypeParameters.isNotEmpty() && stackIterator.hasNext()) {
            val previousExprPsi = stackIterator.next() ?: continue
            // Rolling back by parents, trying to find type arguments.
            // The property setter call is a special case as it's represented as `FirVariableAssignment`
            // and the type arguments should be extracted from its `lvalue`.
            val typeArgumentHolder: FirQualifiedAccessExpression =
                previousExprPsi.parentsWithSelf.firstNotNullOfOrNull { psiElement ->
                    if (psiElement is KtElement) {
                        val fir = psiElement.getOrBuildFir(resolutionFacade)
                        when (fir) {
                            is FirQualifiedAccessExpression -> fir
                            is FirVariableAssignment -> if (fir.lValue is FirQualifiedAccessExpression) {
                                fir.lValue as FirQualifiedAccessExpression
                            } else {
                                null
                            }
                            else -> null
                        }
                    } else {
                        null
                    }
                } ?: continue
            val extractedFromPreviousExpression = extractReifiedTypeArguments(typeArgumentHolder)
            for ((extractedParam, extractedArg) in extractedFromPreviousExpression) {
                if (extractedParam in unmappedTypeParameters) {
                    mapping[extractedParam] = extractedArg
                    unmappedTypeParameters.remove(extractedParam)
                    extractedArg.collectTypeParameters(unmappedTypeParameters)
                }
            }
        }

        return mapping.mapValues { (_, firTypeRef) -> firTypeRef.coneType }
    }

    private fun extractReifiedTypeArguments(typeArgumentsHolder: FirQualifiedAccessExpression): Map<FirTypeParameterSymbol, FirTypeRef> {
        val callableSymbol = typeArgumentsHolder.calleeReference.toResolvedCallableSymbol() ?: return emptyMap()
        return buildMap {
            for ((typeParameterSymbol, typeArgument) in callableSymbol.typeParameterSymbols.zip(typeArgumentsHolder.typeArguments)) {
                if (typeParameterSymbol.isReified && typeArgument is FirTypeProjectionWithVariance) {
                    put(typeParameterSymbol, typeArgument.typeRef)
                }
            }
        }
    }

    private class InjectedSymbolProvider(
        codeFragmentMappings: CodeFragmentMappings,
        private val mainKtFile: KtFile,
    ) {
        val conversionData: CodeFragmentConversionData = codeFragmentMappings.conversionData

        private val injectedValueMapping: Map<CodeFragmentCapturedId, InjectedValue> =
            codeFragmentMappings.injectedValues.associateBy { CodeFragmentCapturedId(it.symbol) }

        fun invoke(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
            val irFile = conversionScope.containingFileIfAny()
            val psiFile = (irFile?.fileEntry as? PsiIrFileEntry)?.psiFile

            if (psiFile != mainKtFile) {
                return null
            }

            val id = when (calleeReference) {
                is FirThisReference -> when (val boundSymbol = calleeReference.boundSymbol) {
                    is FirClassSymbol -> CodeFragmentCapturedId(boundSymbol)
                    is FirReceiverParameterSymbol, is FirValueParameterSymbol -> {
                        when (val referencedSymbol = calleeReference.referencedMemberSymbol) {
                            // Specific (deprecated) case for a class context receiver
                            // TODO: remove with KT-72994
                            is FirClassSymbol -> CodeFragmentCapturedId(referencedSymbol)
                            else -> CodeFragmentCapturedId(boundSymbol)
                        }
                    }
                    is FirTypeParameterSymbol, is FirTypeAliasSymbol -> errorWithFirSpecificEntries(
                        message = "Unexpected FirThisOwnerSymbol ${calleeReference::class.simpleName}", fir = boundSymbol.fir
                    )
                    null -> null
                }
                else -> calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()?.let { CodeFragmentCapturedId(it) }
            }
            return injectedValueMapping[id]
        }
    }

    private class CompilerFacilityJvmGeneratorExtensions(
        private val delegate: JvmGeneratorExtensions
    ) : StubGeneratorExtensions(), JvmGeneratorExtensions by delegate {
        override fun generateRawTypeAnnotationCall(): IrConstructorCall? = delegate.generateRawTypeAnnotationCall()

        /**
         * This method is used from `org.jetbrains.kotlin.backend.jvm.lower.SpecialAccessLowering.visitCall`
         * (via generateReflectiveAccessForGetter) and it is called for the private access member lowered to the getter/setter call.
         * If a private property has no getter/setter (the typical situation for simple private properties without explicitly defined
         * getter/setter), then this method is not used at all. Instead,
         * `org.jetbrains.kotlin.backend.jvm.lower.SpecialAccessLowering.visitGetField` (or visitSetField) generates the access without
         * asking.
         */
        override fun isAccessorWithExplicitImplementation(accessor: IrSimpleFunction): Boolean {
            if (accessor is AbstractFir2IrLazyDeclaration<*>) {
                val fir = accessor.fir
                if (fir is FirFunction && fir.hasBody) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Extensions for the code fragment context transformation.
     * Collects all local declarations of the context file to feed the code fragment transformer later.
     */
    private class CodeFragmentContextFir2IrExtensions(
        delegate: Fir2IrExtensions,
        private val contextDeclarationCache: CodeFragmentContextDeclarationCache?,
    ) : Fir2IrExtensions by delegate {
        override fun preserveLocalScope(symbol: IrSymbol, cache: Fir2IrScopeCache) {
            contextDeclarationCache?.registerLocalScope(symbol, cache)
        }
    }

    /**
     * Extensions for code fragment transformation.
     * This replaces captured value calls with injected parameter calls of the code fragment.
     */
    private class CodeFragmentFir2IrExtensions(
        delegate: Fir2IrExtensions,
        private val injectedValueProvider: InjectedSymbolProvider?,
    ) : Fir2IrExtensions by delegate {
        override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
            return injectedValueProvider?.invoke(calleeReference, conversionScope)
        }

        override fun codeFragmentConversionData(fragment: FirCodeFragment): CodeFragmentConversionData {
            return injectedValueProvider?.conversionData ?: errorWithFirSpecificEntries("Conversion data is not provided", fir = fragment)
        }
    }

    private class SelectedFilesGenerateClassFilter(
        private val files: List<KtFile>,
        private val inlinedClasses: Set<KtClassOrObject>
    ) : GenerationState.GenerateClassFilter() {
        private val filesWithInlinedClasses = inlinedClasses.mapTo(mutableSetOf()) { it.containingKtFile }

        override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
            return ktFile in files || ktFile in filesWithInlinedClasses
        }

        override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
            return processingClassOrObject.containingKtFile in files ||
                    processingClassOrObject is KtObjectDeclaration && processingClassOrObject in inlinedClasses
        }
    }

    private fun createJvmIrCodegenFactory(configuration: CompilerConfiguration, evaluatorData: JvmEvaluatorData?): JvmIrCodegenFactory {
        val jvmGeneratorExtensions = object : JvmGeneratorExtensionsImpl(configuration) {
            override fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? {
                // Stubbed top-level function IR symbols (from other source files in the module) require a parent facade class to be
                // generated, which requires a container source to be provided. Without a facade class, function IR symbols will have
                // an `IrExternalPackageFragment` parent, which trips up code generation during IR lowering.
                val psiSourceFile =
                    descriptor.toSourceElement.containingFile as? PsiSourceFile ?: return super.getContainerSource(descriptor)
                return FacadeClassSourceShimForFragmentCompilation(psiSourceFile)
            }
        }

        val ideCodegenSettings = JvmIrCodegenFactory.IdeCodegenSettings(
            shouldStubAndNotLinkUnboundSymbols = true,
            shouldDeduplicateBuiltInSymbols = false,

            // Because the file to compile may be contained in a "common" multiplatform module, an `expect` declaration doesn't necessarily
            // have an obvious associated `actual` symbol. `shouldStubOrphanedExpectSymbols` generates stubs for such `expect` declarations.
            shouldStubOrphanedExpectSymbols = true,

            // Likewise, the file to compile may be contained in a "platform" multiplatform module, where the `actual` declaration is
            // referenced in the symbol table automatically, but not its `expect` counterpart, because it isn't contained in the files to
            // compile. `shouldReferenceUndiscoveredExpectSymbols` references such `expect` symbols in the symbol table so that they can
            // subsequently be stubbed.
            shouldReferenceUndiscoveredExpectSymbols = false, // TODO it was true

            // Compilation state acts as an in-out container for captured type parameter and local function mappings
            evaluatorData = evaluatorData
        )

        return JvmIrCodegenFactory(
            configuration,
            jvmGeneratorExtensions = jvmGeneratorExtensions,
            ideCodegenSettings = ideCodegenSettings,
        )
    }
}

private class KaFirDependencyCompiledCodeProvider(val cache: Map<String, ByteArray>) : CompiledCodeProvider {
    override fun getClassBytes(className: String): ByteArray? {
        return cache[className]
    }
}

private class KaFirDelegatingCompiledCodeProvider(private val delegates: List<CompiledCodeProvider>) : CompiledCodeProvider {
    override fun getClassBytes(className: String): ByteArray? {
        return delegates.firstNotNullOfOrNull { it.getClassBytes(className) }
    }
}

private class KaFirDirectoryBasedCompiledCodeProvider(private val outputDirectory: VirtualFile) : CompiledCodeProvider {
    override fun getClassBytes(className: String): ByteArray? {
        return outputDirectory.findFile("$className.class")?.contentsToByteArray(false)
    }
}

private class SyntaxErrorReportingVisitor(
    private val useSiteSession: FirSession,
    private val diagnosticConverter: (KtPsiDiagnostic) -> KaDiagnosticWithPsi<*>
) : KtTreeVisitorVoid() {
    private val collectedDiagnostics = mutableListOf<KaDiagnostic>()

    val diagnostics: List<KaDiagnostic>
        get() = Collections.unmodifiableList(collectedDiagnostics)

    override fun visitErrorElement(element: PsiErrorElement) {
        collectedDiagnostics += ConeSyntaxDiagnostic(element.errorDescription)
            .toFirDiagnostics(useSiteSession, KtRealPsiSourceElement(element), callOrAssignmentSource = null)
            .map { diagnosticConverter(it as KtPsiDiagnostic) }

        super.visitErrorElement(element)
    }
}

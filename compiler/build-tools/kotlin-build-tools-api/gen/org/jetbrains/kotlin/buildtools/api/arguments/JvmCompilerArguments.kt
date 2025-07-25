package org.jetbrains.kotlin.buildtools.api.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget

/**
 * @since 2.3.0
 */
public interface JvmCompilerArguments : CommonCompilerArguments {
  /**
   * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
   *
   * @return the previously set value for an option
   * @throws IllegalStateException if the option was not set and has no default value
   */
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V

  /**
   * Set the [value] for option specified by [key], overriding any previous value for that option.
   */
  public operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V)

  /**
   * Base class for [JvmCompilerArguments] options.
   *
   * @see get
   * @see set    
   */
  public class JvmCompilerArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Destination for generated class files.
     */
    @JvmField
    public val D: JvmCompilerArgument<String?> = JvmCompilerArgument("D")

    /**
     * List of directories and JAR/ZIP archives to search for user class files.
     */
    @JvmField
    public val CLASSPATH: JvmCompilerArgument<String?> = JvmCompilerArgument("CLASSPATH")

    /**
     * Include the Kotlin runtime in the resulting JAR.
     */
    @JvmField
    public val INCLUDE_RUNTIME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("INCLUDE_RUNTIME")

    /**
     * Include a custom JDK from the specified location in the classpath instead of the default 'JAVA_HOME'.
     */
    @JvmField
    public val JDK_HOME: JvmCompilerArgument<String?> = JvmCompilerArgument("JDK_HOME")

    /**
     * Don't automatically include the Java runtime in the classpath.
     */
    @JvmField
    public val NO_JDK: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_JDK")

    /**
     * Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath.
     */
    @JvmField
    public val NO_STDLIB: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_STDLIB")

    /**
     * Don't automatically include the Kotlin reflection dependency in the classpath.
     */
    @JvmField
    public val NO_REFLECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_REFLECT")

    /**
     * Script definition template classes.
     */
    @JvmField
    public val SCRIPT_TEMPLATES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("SCRIPT_TEMPLATES")

    /**
     * Name of the generated '.kotlin_module' file.
     */
    @JvmField
    public val MODULE_NAME: JvmCompilerArgument<String?> = JvmCompilerArgument("MODULE_NAME")

    /**
     * The target version of the generated JVM bytecode (1.8 and 9–24), with 1.8 as the default.
     */
    @JvmField
    public val JVM_TARGET: JvmCompilerArgument<JvmTarget?> = JvmCompilerArgument("JVM_TARGET")

    /**
     * Generate metadata for Java 1.8 reflection on method parameters.
     */
    @JvmField
    public val JAVA_PARAMETERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("JAVA_PARAMETERS")

    /**
     * Emit JVM default methods for interface declarations with bodies. The default is 'enable'.
     * -jvm-default=enable              Generate default methods for non-abstract interface declarations, as well as 'DefaultImpls' classes with
     *                                  static methods for compatibility with code compiled in the 'disable' mode.
     *                                  This is the default behavior since language version 2.2.
     * -jvm-default=no-compatibility    Generate default methods for non-abstract interface declarations. Do not generate 'DefaultImpls' classes.
     * -jvm-default=disable             Do not generate JVM default methods. This is the default behavior up to language version 2.1.
     */
    @JvmField
    public val JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("JVM_DEFAULT")

    /**
     * Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_UNSTABLE_DEPENDENCIES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_UNSTABLE_DEPENDENCIES")

    /**
     * When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable
     * to prevent diagnostics from being reported when using stable compilers at the call site.
     * When using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable
     * to force diagnostics to be reported.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ABI_STABILITY: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_ABI_STABILITY")

    /**
     * When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_DO_NOT_CLEAR_BINDING_CONTEXT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT")

    /**
     * Run codegen phase in N parallel threads.
     * 0 means use one thread per processor core.
     * The default value is 1.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_BACKEND_THREADS: JvmCompilerArgument<Int> =
        JvmCompilerArgument("X_BACKEND_THREADS")

    /**
     * Paths to Java 9+ modules.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MODULE_PATH: JvmCompilerArgument<String?> = JvmCompilerArgument("X_MODULE_PATH")

    /**
     * Root modules to resolve in addition to the initial modules, or all modules on the module path if <module> is ALL-MODULE-PATH.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ADD_MODULES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_ADD_MODULES")

    /**
     * Don't generate not-null assertions for arguments of platform types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_CALL_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_CALL_ASSERTIONS")

    /**
     * Don't generate not-null assertions for extension receiver arguments of platform types.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_RECEIVER_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_RECEIVER_ASSERTIONS")

    /**
     * Don't generate not-null assertions on parameters of methods accessible from Java.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_PARAM_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_PARAM_ASSERTIONS")

    /**
     * Disable optimizations.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_OPTIMIZE: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_NO_OPTIMIZE")

    /**
     * 'kotlin.assert' call behavior:
     * -Xassertions=always-enable:  enable, ignore JVM assertion settings;
     * -Xassertions=always-disable: disable, ignore JVM assertion settings;
     * -Xassertions=jvm:            enable, depend on JVM assertion settings;
     * -Xassertions=legacy:         calculate the condition on each call, the behavior depends on JVM assertion settings in the kotlin package;
     * default: legacy
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ASSERTIONS: JvmCompilerArgument<String?> = JvmCompilerArgument("X_ASSERTIONS")

    /**
     * Compile multifile classes as a hierarchy of parts and a facade.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_MULTIFILE_PARTS_INHERIT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_MULTIFILE_PARTS_INHERIT")

    /**
     * Use a type table in metadata serialization.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_TYPE_TABLE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_TYPE_TABLE")

    /**
     * Use the old implementation for reading class files. This may slow down the compilation and cause problems with Groovy interop.
     * This can be used in the event of problems with the new implementation.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_OLD_CLASS_FILES_READING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_OLD_CLASS_FILES_READING")

    /**
     * Use the fast implementation of Jar FS. This may speed up compilation time, but it is experimental.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_FAST_JAR_FILE_SYSTEM: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_USE_FAST_JAR_FILE_SYSTEM")

    /**
     * Suppress the "cannot access built-in declaration" error (useful with '-no-stdlib').
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPRESS_MISSING_BUILTINS_ERROR: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SUPPRESS_MISSING_BUILTINS_ERROR")

    /**
     * Set the script resolver environment in key-value pairs (the value can be quoted and escaped).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SCRIPT_RESOLVER_ENVIRONMENT: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_SCRIPT_RESOLVER_ENVIRONMENT")

    /**
     * Paths to directories with Java source files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JAVA_SOURCE_ROOTS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_JAVA_SOURCE_ROOTS")

    /**
     * Package prefix for Java files.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JAVA_PACKAGE_PREFIX: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_JAVA_PACKAGE_PREFIX")

    /**
     * Specify the behavior of 'JSR-305' nullability annotations:
     * -Xjsr305={ignore/strict/warn}                   global (all non-@UnderMigration annotations)
     * -Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations
     * -Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name
     * Modes:
     * * ignore
     * * strict (experimental; treat like other supported nullability annotations)
     * * warn (report a warning)
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JSR305: JvmCompilerArgument<Array<String>?> = JvmCompilerArgument("X_JSR305")

    /**
     * Specify the behavior for specific Java nullability annotations (provided with fully qualified package name).
     * Modes:
     * * ignore
     * * strict
     * * warn (report a warning)
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NULLABILITY_ANNOTATIONS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_NULLABILITY_ANNOTATIONS")

    /**
     * Specify the behavior for Checker Framework 'compatqual' annotations ('NullableDecl'/'NonNullDecl').
     * The default value is 'enable'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")

    /**
     * Specify the behavior of 'jspecify' annotations.
     * The default value is 'warn'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JSPECIFY_ANNOTATIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_JSPECIFY_ANNOTATIONS")

    /**
     * This option is deprecated. Migrate to -jvm-default as follows:
     * -Xjvm-default=disable            -> -jvm-default=disable
     * -Xjvm-default=all-compatibility  -> -jvm-default=enable
     * -Xjvm-default=all                -> -jvm-default=no-compatibility
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JVM_DEFAULT")

    /**
     * Compile expressions and unrecognized scripts passed with the -script argument as scripts with the given filename extension.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DEFAULT_SCRIPT_EXTENSION: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_DEFAULT_SCRIPT_EXTENSION")

    /**
     * Disable standard Kotlin scripting support.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DISABLE_STANDARD_SCRIPT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_DISABLE_STANDARD_SCRIPT")

    /**
     * Generate metadata with strict version semantics (see the KDoc entry on 'Metadata.extraInt').
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_GENERATE_STRICT_METADATA_VERSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_GENERATE_STRICT_METADATA_VERSION")

    /**
     * Transform '(' and ')' in method names to some other character sequence.
     * This mode can BREAK BINARY COMPATIBILITY and should only be used as a workaround for
     * problems with parentheses in identifiers on certain platforms.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SANITIZE_PARENTHESES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SANITIZE_PARENTHESES")

    /**
     * Paths to output directories for friend modules (modules whose internals should be visible).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_FRIEND_PATHS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_FRIEND_PATHS")

    /**
     * Allow the set of source files to be empty.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ALLOW_NO_SOURCE_FILES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_NO_SOURCE_FILES")

    /**
     * Emit JVM type annotations in bytecode.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_EMIT_JVM_TYPE_ANNOTATIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_EMIT_JVM_TYPE_ANNOTATIONS")

    /**
     * Expose inline classes and functions, accepting and returning them, to Java.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JVM_EXPOSE_BOXED: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_EXPOSE_BOXED")

    /**
     * Select the code generation scheme for string concatenation:
     * -Xstring-concat=indy-with-constants  Concatenate strings using 'invokedynamic' and 'makeConcatWithConstants'. This requires '-jvm-target 9' or greater.
     * -Xstring-concat=indy                 Concatenate strings using 'invokedynamic' and 'makeConcat'. This requires '-jvm-target 9' or greater.
     * -Xstring-concat=inline               Concatenate strings using 'StringBuilder'
     * default: 'indy-with-constants' for JVM targets 9 or greater, 'inline' otherwise.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_STRING_CONCAT: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_STRING_CONCAT")

    /**
     * Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.
     * The supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9–24.
     * This also sets the value of '-jvm-target' to be equal to the selected JDK version.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JDK_RELEASE: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JDK_RELEASE")

    /**
     * Select the code generation scheme for SAM conversions.
     * -Xsam-conversions=indy          Generate SAM conversions using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
     * -Xsam-conversions=class         Generate SAM conversions as explicit classes.
     * The default value is 'indy'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SAM_CONVERSIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_SAM_CONVERSIONS")

    /**
     * Select the code generation scheme for lambdas.
     * -Xlambdas=indy                  Generate lambdas using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
     *                                 A lambda object created using 'LambdaMetafactory.metafactory' will have a different 'toString()'.
     * -Xlambdas=class                 Generate lambdas as explicit classes.
     * The default value is 'indy' if language version is 2.0+, and 'class' otherwise.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LAMBDAS: JvmCompilerArgument<String?> = JvmCompilerArgument("X_LAMBDAS")

    /**
     * Allow using 'invokedynamic' for lambda expressions with annotations
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_INDY_ALLOW_ANNOTATED_LAMBDAS: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_INDY_ALLOW_ANNOTATED_LAMBDAS")

    /**
     * Paths to cross-platform libraries in the .klib format.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_KLIB: JvmCompilerArgument<String?> = JvmCompilerArgument("X_KLIB")

    /**
     * Don't reset jar entry timestamps to a fixed date.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_RESET_JAR_TIMESTAMPS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_RESET_JAR_TIMESTAMPS")

    /**
     * Use pre-1.4 exception types instead of 'java.lang.NPE' in null checks. See KT-22275 for more details.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_UNIFIED_NULL_CHECKS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_UNIFIED_NULL_CHECKS")

    /**
     * Don't generate the '@kotlin.jvm.internal.SourceDebugExtension' annotation with an SMAP copy on classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_SOURCE_DEBUG_EXTENSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_SOURCE_DEBUG_EXTENSION")

    /**
     * Debug option: Run the compiler with the async profiler and save snapshots to `outputDir`; `command` is passed to the async profiler on start.
     * `profilerPath` is the path to libasyncProfiler.so; async-profiler.jar should be on the compiler classpath.
     * If it's not on the classpath, the compiler will attempt to load async-profiler.jar from the containing directory of profilerPath.
     * Example: -Xprofile=<PATH_TO_ASYNC_PROFILER>/async-profiler/build/libasyncProfiler.so:event=cpu,interval=1ms,threads,start:<SNAPSHOT_DIR_PATH>
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_PROFILE: JvmCompilerArgument<String?> = JvmCompilerArgument("X_PROFILE")

    /**
     * Use the scheme for inline class mangling from version 1.4 instead of the one from 1.4.30.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_14_INLINE_CLASSES_MANGLING_SCHEME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")

    /**
     * Allow using Java features that are in the preview phase.
     * This works like '--enable-preview' in Java. All class files are marked as compiled with preview features, meaning it won't be possible to use them in release environments.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_JVM_ENABLE_PREVIEW: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_ENABLE_PREVIEW")

    /**
     * Suppress warnings about deprecated JVM target versions.
     * This option has no effect and will be deleted in a future version.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING")

    /**
     * Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,
     * including the ability to read type-use annotations from class files.
     * See KT-45671 for more details.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE")

    /**
     * Save the IR to metadata (Experimental).
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_SERIALIZE_IR: JvmCompilerArgument<String> = JvmCompilerArgument("X_SERIALIZE_IR")

    /**
     * Validate generated JVM bytecode before and after optimizations.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VALIDATE_BYTECODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALIDATE_BYTECODE")

    /**
     * Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any').
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL")

    /**
     * Link JVM IR symbols via signatures instead of descriptors.
     * This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.
     * This option is deprecated and will be deleted in future versions.
     * It has no effect when -language-version is 2.0 or higher.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_LINK_VIA_SIGNATURES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_LINK_VIA_SIGNATURES")

    /**
     * Enable debug mode for compilation.
     * Currently this includes spilling all variables in a suspending context regardless of whether they are alive.
     * If API Level >= 2.2 -- no-op.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_DEBUG: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_DEBUG")

    /**
     * Generate additional linenumber instruction for compiler-generated code
     * inside suspend functions and lambdas to distinguish them from user code by debugger.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ENHANCED_COROUTINES_DEBUGGING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ENHANCED_COROUTINES_DEBUGGING")

    /**
     * Don't generate Java 1.8+ targets for Kotlin annotation classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_NO_NEW_JAVA_ANNOTATION_TARGETS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_NEW_JAVA_ANNOTATION_TARGETS")

    /**
     * Enable experimental value classes.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_VALUE_CLASSES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALUE_CLASSES")

    /**
     * Inline functions using the IR inliner instead of the bytecode inliner.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_IR_INLINER: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_IR_INLINER")

    /**
     * Use inline scopes numbers for inline marker variables.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_INLINE_SCOPES_NUMBERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_INLINE_SCOPES_NUMBERS")

    /**
     * Enable the experimental support for K2 KAPT.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_USE_K2_KAPT: JvmCompilerArgument<Boolean?> = JvmCompilerArgument("X_USE_K2_KAPT")

    /**
     * Enable behaviour needed to compile builtins as part of JVM stdlib
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_COMPILE_BUILTINS_AS_PART_OF_STDLIB: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB")

    /**
     * Output builtins metadata as .kotlin_builtins files
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_OUTPUT_BUILTINS_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_OUTPUT_BUILTINS_METADATA")

    /**
     * Write annotations on declarations into the metadata (in addition to the JVM bytecode), and read annotations from the metadata if they are present.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_ANNOTATIONS_IN_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ANNOTATIONS_IN_METADATA")

    /**
     * Select the code generation scheme for type-checking 'when' expressions:
     * -Xwhen-expressions=indy         Generate type-checking 'when' expressions using 'invokedynamic' with 'SwitchBootstraps.typeSwitch(..)' and 
     *                                 following 'tableswitch' or 'lookupswitch'. This requires '-jvm-target 21' or greater.
     * -Xwhen-expressions=inline       Generate type-checking 'when' expressions as a chain of type checks.
     * The default value is 'inline'.
     *
     * WARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.
     */
    @JvmField
    @ExperimentalCompilerArgument
    public val X_WHEN_EXPRESSIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_WHEN_EXPRESSIONS")
  }
}

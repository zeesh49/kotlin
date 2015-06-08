# -injars '../../ideaSDK/lib/idea.jar'
# -injars '../../ideaSDK/lib/openapi.jar'

-injars '../../out/artifacts/internal/formatter.jar'

#-injars '../../ideaSDK/core-analysis/intellij-core-analysis.jar'(!META-INF/*,!**.html)
#-injars '../../ideaSDK/core/annotations.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/asm-all.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/guava-17.0.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/jdom.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/jna.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/jsr166e.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/log4j.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/picocontainer.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/trove4j.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/util.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/xpp3-1.1.4-min.jar'(!META-INF/*)
#-injars '../../ideaSDK/core/xstream-1.4.3.jar'(!META-INF/*)
#-injars '../../ideaSDK/plugins/gradle/lib/jsr305-1.3.9.jar'(!META-INF/*)
#-injars '../../ideaSDK/lib/automaton.jar'(!META-INF/*)
#-injars '../../ideaSDK/lib/jna-utils.jar'(!META-INF/*)
#-injars '../../ideaSDK/lib/jps-model.jar'(!META-INF/*)
#-injars '../../ideaSDK/lib/snappy-in-java-0.3.1.jar'(!META-INF/*)

-injars '../../out/artifacts/internal/formatter-all-before.jar'
-outjars '../../out/artifacts/internal/formatter-all.jar'

-libraryjars '../../dist/kotlinc/lib/kotlin-compiler.jar'
-libraryjars '<rtjar>'
-libraryjars '<jssejar>'

-dontnote **
-dontwarn com.intellij.codeInsight.**
-dontwarn com.intellij.codeInsight.completion.**
-dontwarn com.intellij.debugger.**
-dontwarn com.intellij.testFramework.**
-dontwarn com.thoughtworks.**
-dontwarn com.intellij.ide.**
-dontwarn org.jetbrains.ide.**
-dontwarn org.apache.**
-dontwarn net.sf.cglib.**
-dontwarn com.intellij.util.xml.**
-dontwarn **TestCase
-dontwarn com.intellij.util.net.**
-dontwarn com.intellij.openapi.vcs.**
-dontwarn org.jdom.xpath.**
-dontwarn **.ui.**
-dontwarn com.intellij.compiler.**
-dontwarn com.intellij.execution.**
-dontwarn com.intellij.refactoring.**
-dontwarn com.intellij.util.CompressionUtil
-dontwarn com.intellij.usages.impl.**
-dontwarn com.intellij.util.InstanceofCheckerGenerator*
-dontwarn com.intellij.util.AppleHiDPIScaledImage*
-dontwarn com.intellij.mock.MockPsiDirectory
-dontwarn com.intellij.find.FindManagerTestUtils

-target 1.6
-dontoptimize
-dontobfuscate

-keep class org.jetbrains.kotlin.idea.core.formatter.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.idea.formatter.** {
    public protected *;
}

-keep class com.intellij.formatting.** {
    public protected *;
}

-injars '/formatter.jar'
-injars 'ideaSDK/lib/openapi.jar'

-outjars '/lib/kotlin-compiler.jar'

-libraryjars '<rtjar>'
-libraryjars '<jssejar>'
-libraryjars '<bootstrap.runtime>'

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

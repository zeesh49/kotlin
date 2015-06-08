-injars '../../ideaSDK/lib/junit-4.11.jar'

-outjars '../../out/artifacts/internal/junit-all.jar'

-libraryjars '../../ideaSDK/lib/hamcrest-core-1.3.jar'
-libraryjars '<rtjar>'
-libraryjars '<jssejar>'

-dontnote **

-target 1.6
-dontoptimize
-dontobfuscate

#-keep class org.jetbrains.kotlin.idea.core.formatter.** {
#    public protected *;
#}
#
#-keep class org.jetbrains.kotlin.idea.formatter.** {
#    public protected *;
#}

-keep class junit.runner.Version {
    public protected *;
}

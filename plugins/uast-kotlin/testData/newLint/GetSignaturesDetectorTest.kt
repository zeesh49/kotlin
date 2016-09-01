import android.content.pm.PackageManager.*

import android.app.Activity
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

class GetSignaturesSingleFlagTest : Activity() {
    @Throws(Exception::class)
    fun failLintCheck() {
        packageManager.getPackageInfo("some.pkg", PackageManager.GET_SIGNATURES)
    }
}

class GetSignaturesBitwiseOrTest : Activity() {
    fun failLintCheck() {
        packageManager.getPackageInfo("some.pkg", GET_GIDS or GET_SIGNATURES or GET_PROVIDERS)
    }
}

class GetSignaturesBitwiseXorTest : Activity() {
    @Throws(Exception::class)
    fun failLintCheck() {
        packageManager.getPackageInfo("some.pkg", PackageManager.GET_SIGNATURES xor 0x0)
    }
}

class GetSignaturesBitwiseAndTest : Activity() {
    @Throws(Exception::class)
    fun failLintCheck() {
        packageManager.getPackageInfo("some.pkg",
                                      Integer.MAX_VALUE and PackageManager.GET_SIGNATURES)
    }
}

class GetSignaturesStaticFieldTest : Activity() {
    @Throws(Exception::class)
    fun failLintCheck() {
        packageManager.getPackageInfo("some.pkg", FLAGS)
    }

    companion object {
        private val FLAGS = PackageManager.GET_SIGNATURES
    }
}

class GetSignaturesLocalVariableTest : Activity() {
    fun passLintCheck() {
        val flags = PackageManager.GET_SIGNATURES
        packageManager.getPackageInfo("some.pkg", flags)
    }
}

class GetSignaturesNoFlagTest : Activity() {
    @Throws(Exception::class)
    fun passLintCheck() {
        packageManager.getPackageInfo("some.pkg",
                                      GET_ACTIVITIES or
                                              GET_GIDS or
                                              GET_CONFIGURATIONS or
                                              GET_INSTRUMENTATION or
                                              GET_PERMISSIONS or
                                              GET_PROVIDERS or
                                              GET_RECEIVERS or
                                              GET_SERVICES or
                                              GET_UNINSTALLED_PACKAGES)
    }
}

class GetSignaturesNotPackageManagerTest : Activity() {
    fun passLintCheck(mock: Mock) {
        mock.getPackageInfo("some.pkg", PackageManager.GET_SIGNATURES)
    }

    interface Mock {
        fun getPackageInfo(pkg: String, flags: Int): PackageInfo
    }
}
package test.pkg

import android.annotation.SuppressLint
import java.io.File

import android.content.Intent
import android.net.Uri

class SdCardTest {
    internal var deviceDir = File("/sdcard/vr")

    init {
        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing("/sdcard/launcher")
        }
        if (File("/sdcard").exists()) {
        }
        val FilePath = "/sdcard/" + File("test")
        System.setProperty("foo.bar", "file://sdcard")


        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(Uri.parse("file://sdcard/foo.json"), "application/bar-json")
        intent.putExtra("path-filter", "/sdcard(/.+)*")
        intent.putExtra("start-dir", "/sdcard")
        val mypath = "/data/data/foo"
        val base = "/data/data/foo.bar/test-profiling"
        val s = "file://sdcard/foo"
    }

    companion object {
        private val PROFILE_STARTUP = true
        private val SDCARD_TEST_HTML = "/sdcard/test.html"
        val SDCARD_ROOT = "/sdcard"
        val PACKAGES_PATH = "/sdcard/o/packages/"
    }
}

@SuppressWarnings("unused")
class SuppressTest5 {
    private fun suppressVariable(): String {
        @SuppressLint("SdCardPath")
        val string = "/sdcard/mypath1"
        return string
    }

    @SuppressLint("SdCardPath")
    private fun suppressMethod(): String {
        val string = "/sdcard/mypath2"
        return string
    }

    @SuppressLint("SdCardPath")
    private class SuppressClass {
        private fun suppressMethod(): String {
            val string = "/sdcard/mypath3"
            return string
        }
    }

    private fun suppressAll(): String {
        @SuppressLint("all")
        val string = "/sdcard/mypath4"
        return string
    }

    private fun suppressCombination(): String {
        @SuppressLint("foo1", "foo2", "SdCardPath")
        val string = "/sdcard/mypath5"

        // This is NOT annotated and *should* generate
        // a warning (here to make sure we don't just
        // suppress everything when we see an annotation
        val notAnnotated = "/sdcard/mypath"

        return string
    }

    private fun suppressWarnings(): String {
        @SuppressWarnings("all")
        val string = "/sdcard/mypath6"

        @SuppressWarnings("SdCardPath")
        val string2 = "/sdcard/mypath7"

        @SuppressWarnings("AndroidLintSdCardPath")
        val string3 = "/sdcard/mypath9"

        //noinspection AndroidLintSdCardPath
        val string4 = "/sdcard/mypath9"

        return string
    }

    @SuppressLint("SdCardPath")
    private val supressField = "/sdcard/mypath8"
}

annotation class MyInterface(val engineer: String = "/sdcard/this/is/wrong")

// Annotation parameter annotations are not supported in Kotlin light classes now
annotation class MyInterface2(@SuppressLint("SdCardPath") val engineer: String = "/sdcard/this/is/wrong")


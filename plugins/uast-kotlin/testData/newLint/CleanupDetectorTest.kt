@file:Suppress("unused")
package test.pkg

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.app.FragmentManager
import android.app.FragmentTransaction
import android.app.Activity
import android.os.Bundle
import android.widget.Toast

abstract class SharedPrefsTest0 : Context() {
    private val editor: SharedPreferences.Editor
        get() = preferences.edit()

    private fun editAndCommit(): Boolean {
        return preferences.edit().commit()
    }

    private val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)
}

@SuppressWarnings("unused")
class CommitTest2 {
    private fun navigateToFragment(transaction: FragmentTransaction?,
                                   supportFragmentManager: FragmentManager) {
        var transaction = transaction
        if (transaction == null) {
            transaction = supportFragmentManager.beginTransaction()
        }

        transaction!!.commit()
    }
}

class CommitTest {
    private val mActivity: Context? = null
    fun selectTab1() {
        var trans: FragmentTransaction? = null
        if (mActivity is Activity) {
            trans = (mActivity as Activity).getFragmentManager().beginTransaction().disallowAddToBackStack()
        }

        if (trans != null && !trans.isEmpty) {
            trans.commit()
        }
    }

    fun select(fragmentManager: FragmentManager) {
        val trans = fragmentManager.beginTransaction().disallowAddToBackStack()
        trans.commit()
    }
}

object Chained {
    private fun falsePositive(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("wat", "wat").commit()
    }

    private fun falsePositive2(context: Context) {
        val `var` = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("wat", "wat").commit()
    }

    private fun truePositive(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("wat", "wat")
    }
}

class SharedPrefsTest8 : Activity() {
    fun commitWarning1() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.commit()
    }

    fun commitWarning2() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        val b = editor.commit() // OK: reading return value
    }

    fun commitWarning3() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        val c: Boolean
        c = editor.commit() // OK: reading return value
    }

    fun commitWarning4() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        if (editor.commit()) { // OK: reading return value
            //noinspection UnnecessaryReturnStatement
            return
        }
    }

    fun commitWarning5() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        var c = false
        c = c or editor.commit() // OK: reading return value
    }

    fun commitWarning6() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        foo(editor.commit()) // OK: reading return value
    }

    fun foo(x: Boolean) {
    }

    fun noWarning() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.apply()
    }
}

class SharedPrefsTest7 {
    internal fun getSharedPreferences(key: String, deflt: Int): SharedPreferences? {
        return null
    }

    fun test(myPrefValue: String) {
        val settings = getSharedPreferences(PREF_NAME, 0)
        settings!!.edit().putString(MY_PREF_KEY, myPrefValue)
    }

    companion object {
        private val PREF_NAME = "MyPrefName"
        private val MY_PREF_KEY = "MyKey"
    }
}

class SharedPrefsFormat {
    fun test(sessionContext: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(sessionContext)
        val nameKey = sessionContext.getString(R.string.pref_key_assigned_bluetooth_device_name)
        val addressKey = sessionContext.getString(R.string.pref_key_assigned_bluetooth_device_address)
        val name = prefs.getString(nameKey, null)
        val address = prefs.getString(addressKey, null)
    }

    class R {
        object string {
            val pref_key_assigned_bluetooth_device_name = 0x7f0a000e
            val pref_key_assigned_bluetooth_device_address = 0x7f0a000f
        }
    }
}

internal class SharedPrefsTest5 {
    var mPreferences: SharedPreferences? = null

    private fun wrong() {
        // Field reference to preferences
        mPreferences!!.edit().putString(PREF_FOO, "bar")
        mPreferences!!.edit().remove(PREF_BAZ).remove(PREF_FOO)
    }

    private fun ok() {
        mPreferences!!.edit().putString(PREF_FOO, "bar").commit()
        mPreferences!!.edit().remove(PREF_BAZ).remove(PREF_FOO).commit()
    }

    private fun wrong2(preferences: SharedPreferences) {
        preferences.edit().putString(PREF_FOO, "bar")
        preferences.edit().remove(PREF_BAZ).remove(PREF_FOO)
    }

    private fun wrong3(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(PREF_FOO, "bar")
        preferences.edit().remove(PREF_BAZ).remove(PREF_FOO)
    }

    private fun wrong4(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit().putString(PREF_FOO, "bar")
    }

    private fun ok2(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(PREF_FOO, "bar").commit()
    }

    private val mPrefs: SharedPreferences? = null

    fun ok3() {
        val editor = mPrefs!!.edit().putBoolean(
                PREF_FOO, true)
        editor.putString(PREF_BAZ, "")
        editor.apply()
    }

    companion object {
        private val PREF_FOO = "foo"
        private val PREF_BAZ = "bar"
    }
}

class SharedPrefsTest4 : Activity() {
    fun test(preferences: SharedPreferences) {
        val editor = preferences.edit()
    }
}

class SharedPrefsTest3 : Activity() {
    fun test(preferences: SharedPreferences) {
        val editor = preferences.edit()
    }
}

class SharedPrefsTest2 : Activity() {
    fun test1(preferences: SharedPreferences) {
        val editor = preferences.edit()
    }

    fun test2(preferences: SharedPreferences) {
        val editor = preferences.edit()
    }
}

class SharedPrefsTest// Constructor test
(context: Context) : Activity() {
    // OK 1
    fun onCreate1(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
        editor.commit()
    }

    // OK 2
    fun onCreate2(savedInstanceState: Bundle, apply: Boolean) {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
        if (apply) {
            editor.apply()
        }
    }

    // OK 3
    fun test1(savedInstanceState: Bundle): Boolean {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
        editor.apply()
        return true
    }

    // Not a bug
    fun test(foo: Foo) {
        val bar1 = foo.edit()
        val bar2 = Foo.edit()
        val bar3 = edit()
    }

    // Bug
    fun bug1(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
    }

    // Bug
    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putString("foo", "bar")
    }

    private fun edit(): Bar? {
        return null
    }

    object Foo {
        internal fun edit(): Bar? {
            return null
        }
    }

    class Bar
}
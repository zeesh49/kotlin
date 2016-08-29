package test.pkg

import android.support.annotation.CallSuper

@SuppressWarnings("UnusedDeclaration")
class CallSuperTest {
    private class Child : Parent() {
        override fun test1() {} // ERROR
        override fun test2() {} // ERROR
        override fun test3() {} // ERROR
        override fun test4(arg: Int) {} // ERROR
        override fun test4(arg: String) {} // OK

        // ERROR
        override fun test5(arg1: Int, arg2: Boolean, arg3: Map<List<String>, *>, arg4: Array<IntArray>, vararg arg5: Int) {

        }

        override fun test5() { // ERROR
            super.test6()
        }

        override fun test6() {
            val x = 5
            super.test5()
            super.test6()
            println(x)
        }
    }

    open class Parent : ParentParent() {
        @CallSuper
        open fun test1() {
        }

        override fun test3() {
            super.test3()
        }

        @CallSuper
        open fun test4(arg: Int) {}

        open fun test4(arg: String) {}

        @CallSuper
        open fun test5() {}

        @CallSuper
        open fun test5(arg1: Int, arg2: Boolean, arg3: Map<List<String>, *>,
                       arg4: Array<IntArray>, vararg arg5: Int) {
        }
    }

    open class ParentParent : ParentParentParent() {
        @CallSuper
        open fun test2() {}

        @CallSuper
        open fun test3() {}

        @CallSuper
        open fun test6() {}

        @CallSuper
        open fun test7() {}
    }

    open class ParentParentParent
}

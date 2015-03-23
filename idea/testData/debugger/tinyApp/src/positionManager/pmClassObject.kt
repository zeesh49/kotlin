package pmClassObject

class A {
    companion object {

        init {
            //Breakpoint!
            1 + 1 // pmClassObject.A
            //Breakpoint!
            val a = 1 // pmClassObject.A
            fun foo() {
                //Breakpoint!
                1 // pmClassObject.A\$Companion\$1
            }
            foo()
        }

        //Breakpoint!
        val prop = 1 // pmClassObject.A

        val prop2: Int
            get() {
                //Breakpoint!
                val a = 1 + 1  // pmClassObject.A\$Companion
                //Breakpoint!
                return 1 // pmClassObject.A\$Companion
            }

        val prop3: Int
            //Breakpoint!
            get() = 1 // pmClassObject.A\$Companion

        //Breakpoint!
        fun foo() = 1 // pmClassObject.A\$Companion

        fun foo2() {
            //Breakpoint!
            ""   // pmClassObject.A\$Companion

            val o = object {
                //Breakpoint!
                val p = 1 // pmClassObject.A\$Companion\$foo2\$o\$1
                val p2: Int
                    get() {
                        //Breakpoint!
                        return 1 // pmClassObject.A\$Companion\$foo2\$o\$1
                    }
            }
            o.p2
        }
    }
}

interface T {
    companion object {
        //Breakpoint!
        val prop = 1 // pmClassObject.T\$Companion
    }
}

fun main(args: Array<String>) {
    A.prop
    A.prop2
    A.prop3
    A.foo()
    A.foo2()
    T.prop
}

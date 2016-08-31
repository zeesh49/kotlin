import android.annotation.SuppressLint
import android.app.Fragment

class Parent {
    fun method(): Fragment {
        return object : Fragment() {

        }
    }
}

@SuppressWarnings("unused")
class FragmentTest {

    // Should be public
    private class Fragment1 : Fragment()

    // Should be static
    inner class Fragment2 : Fragment()

    // Should have a public constructor
    class Fragment3 private constructor() : Fragment()

    // Should have a public constructor with no arguments
    class Fragment4 private constructor(dummy: Int) : Fragment()

    // Should *only* have the default constructor, not the
    // multi-argument one
    class Fragment5 : Fragment {
        constructor() {
        }

        constructor(dummy: Int) {
        }
    }

    // Suppressed
    @SuppressLint("ValidFragment")
    class Fragment6 private constructor() : Fragment()

    class ValidFragment1 : Fragment()

    // (Not a fragment)
    private inner class NotAFragment

    // Ok: Has implicit constructor
    class Fragment7 : Fragment()
}

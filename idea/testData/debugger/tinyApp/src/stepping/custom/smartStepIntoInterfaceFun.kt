// KT-13485
package smartStepIntoInterfaceFun

interface ObjectFace {
    fun act()
}

class ObjectClass : ObjectFace {
    override fun act() {
        println()
    }
}

fun main(args: Array<String>) {
    val simple: ObjectFace = ObjectClass()
    // SMART_STEP_INTO_BY_INDEX: 1
    //Breakpoint!
    simple.act()
}

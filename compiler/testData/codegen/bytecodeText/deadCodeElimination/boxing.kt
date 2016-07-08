class A

fun foo(x: Any?) {}

fun box() {
    val x: Int? = 1
    x!!

    var p1 = 1
    var p2 = 1
    val z: Int? = if (p1 == p2) x else null
    z!!

    foo(1 as java.lang.Integer)
    
    val y: Any? = if (p1 == p2) x else A()
    y!!
}

// 0 IFNULL
// 1 IFNONNULL
// 1 throwNpe
// 0 ATHROW

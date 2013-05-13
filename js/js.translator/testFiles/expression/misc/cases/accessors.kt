package foo

class A {
  val a = 0
  var b = "1"
/*  fun a() = 2
  fun b() = "3"
  fun b_2() = "4"
*/
  var foo: Int = 5
    get() = $foo
    set(v) = $foo = v
}

fun A.bar() = foo
val A.baz: String
    get() = b

fun box(): Boolean {
  val a = A()
  return a.a == 0 && a.b == "1" && a.bar() == 5 && a.baz == "1"
}
package foo

import js.*

native
fun paramCount(vararg a : Int) : Int = js.noImpl

// test spread operator
fun count(vararg a : Int) = paramCount(*a)

native
class A(val size: Int, order: Int = 0) {
  fun test(order: Int, dummy: Int, vararg args: Int): Boolean = js.noImpl
  class object {
    fun startNewTest(): Boolean = js.noImpl
    var hasOrderProblem: Boolean = false
  }
}

native
object b {
  fun test(size: Int, vararg args: Int): Boolean = js.noImpl
}

fun spreadInMethodCall(size: Int, vararg args: Int) = A(size).test(0, 1, *args)

fun spreadInObjectMethodCall(size: Int, vararg args: Int) = b.test(size, *args)

native
fun testNativeVarargWithFunLit(vararg args: Int, f: (a: IntArray) -> Boolean): Boolean = js.noImpl

fun testSpreadOperatorWithSafeCall(a: A?, expected: Boolean?, vararg args: Int): Boolean {
  return a?.test(0, 1, *args) == expected
}

fun testSpreadOperatorWithSureCall(a: A?, vararg args: Int): Boolean {
  return a!!.test(0, 1, *args)
}

fun testCallOrder(vararg args: Int) =
    A.startNewTest() &&
    A(args.size, 0).test(1, 1, *args) && A(args.size, 2).test(3, 1, *args) &&
    !A.hasOrderProblem

fun box() =
  paramCount(1, 2 ,3) == 3 &&
  paramCount() == 0 &&
  count() == 0 &&
  count(1, 1, 1, 1) == 4 &&
  A.startNewTest() && A(5).test(0, 1, 1, 2, 3, 4, 5) && !A.hasOrderProblem &&
  spreadInMethodCall(2, 1, 2) &&
  b.test(5, 1, 2, 3, 4, 5) &&
  spreadInObjectMethodCall(2, 1, 2) &&
  testNativeVarargWithFunLit(1, 2, 3) { args -> args.size == 3 } &&
  testSpreadOperatorWithSafeCall(null, null) &&
  testSpreadOperatorWithSafeCall(A(3), true, 1, 2, 3) &&
  testSpreadOperatorWithSureCall(A(3), 1, 2, 3) &&
  testCallOrder()
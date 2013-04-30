package foo

fun testSize(expectedSize : Int, vararg i : Int) : Boolean {
    return (i.size == expectedSize)
}

fun testSum(expectedSum : Int, vararg i : Int) : Boolean {
    var sum = 0
  for (j in i) {
    sum += j
  }

  return (expectedSum == sum)
}

fun testSpreadOperator(vararg args : Int): Boolean {
  var sum = 0;
  for (a in args) sum += a

  return testSize(args.size, *args) && testSum(sum, *args)
}

class A(val size: Int, val sum: Int) {
  fun test(vararg args : Int) = testSize(size, *args) && testSum(sum, *args)
}

object b {
  fun test(size: Int, sum: Int, vararg args: Int) = testSize(size, *args) && testSum(sum, *args)
}

fun spreadInMethodCall(size: Int, sum: Int, vararg args: Int) = A(size, sum).test(*args)

fun spreadInObjectMethodCall(size: Int, sum: Int, vararg args: Int) = b.test(size, sum, *args)

fun testVarargWithFunLit(vararg args: Int, f: (a: IntArray) -> Boolean): Boolean = f(args)

fun box() =
  testSize(0) &&
  testSum(0) &&
  testSize(3, 1, 1, 1) &&
  testSum(3, 1, 1, 1) &&
  testSize(6, 1, 1, 1, 2, 3, 4) &&
  testSum(30, 10, 20, 0) &&
  testSpreadOperator(30, 10, 20, 0) &&
  A(3, 30).test(10, 20, 0) &&
  spreadInMethodCall(2, 3, 1, 2) &&
  b.test(5, 15, 1, 2, 3, 4, 5) &&
  spreadInObjectMethodCall(2, 3, 1, 2) &&
  testVarargWithFunLit(1, 2, 3) { args -> args.size == 3 }
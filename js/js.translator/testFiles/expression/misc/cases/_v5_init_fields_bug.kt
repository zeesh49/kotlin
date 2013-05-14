val a = "a"
val ab = a + "b"

fun box(): Boolean {
  return a == "a" && ab == "ab"
}
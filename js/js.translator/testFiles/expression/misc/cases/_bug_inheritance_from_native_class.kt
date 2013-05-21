package foo

native
trait User {
  val name: String
  val age: Int
}
class A(override val name: String, override val age: Int) : User

fun box(): Boolean {
  val u = A("name", 1)
  return u.name == "name" && u.age == 1;
}
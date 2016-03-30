package demo

import org.testng.Assert.assertEquals

class GreeterTest {
    fun test() {
       val greeter = Greeter("Hi!")
        assertEquals("Hi!", greeter.greeting)
    }
}
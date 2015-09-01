class TestFromJava() : BaseJava<String>() {
    override fun foo(list: List<String>) {
    }
}

fun <T> test(base: BaseJava<T>) {
    base.foo<caret>(testListOf())
}

fun <T> testListOf(): List<T> = null!!

// REF: (in BaseJava).foo(List<T>)
// REF: (in TestFromJava).foo(List<String>)
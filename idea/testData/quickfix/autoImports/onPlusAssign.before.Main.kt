// "Import" "true"
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String defined in kotlin

package testing

import some.Some

fun testing() {
    var s = Some()
    s += 1
}

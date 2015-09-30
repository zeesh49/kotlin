// "Import" "true"
// ERROR: 'Some()' of type 'some.Some' cannot be invoked as a function. The function invoke() is not found

package testing

import some.Some

fun testing() {
    Some()<caret>("str")
}

// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -USELESS_ELVIS

fun test() {
    bar(<!ARGUMENT_TYPE_MISMATCH!>if (true) {
        1
    } else {
        2
    }<!>)

    bar(<!ARGUMENT_TYPE_MISMATCH!>1 ?: 2<!>)
}

fun bar(s: String) = s

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, ifExpression, integerLiteral */

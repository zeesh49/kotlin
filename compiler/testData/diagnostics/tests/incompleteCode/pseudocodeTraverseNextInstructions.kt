// RUN_PIPELINE_TILL: FRONTEND
package b

fun foo() {
    for (i in <!UNRESOLVED_REFERENCE!>collection<!>) {
        {
         <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
    }
}<!SYNTAX!><!>

/* GENERATED_FIR_TAGS: break, forLoop, functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */

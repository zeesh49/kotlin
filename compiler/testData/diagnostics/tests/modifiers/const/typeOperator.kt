// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INCOMPATIBLE_TYPES -USELESS_CAST

class MyClass

const val asOperator1 = 1 as Int
const val asOperator2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 <!CAST_NEVER_SUCCEEDS!>as<!> String<!>
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val asOperator3 = "1" <!CAST_NEVER_SUCCEEDS!>as?<!> Int
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val asOperator4 = "1" <!CAST_NEVER_SUCCEEDS!>as?<!> MyClass
const val asOperator5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>f() as String<!>

const val isOperator1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, USELESS_IS_CHECK!>1 is Int<!>
const val isOperator2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 is String<!>
const val isOperator3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"1" !is Int<!>
const val isOperator4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"1" !is MyClass<!>

fun f(): String = ""

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, const, functionDeclaration, integerLiteral, isExpression,
nullableType, propertyDeclaration, stringLiteral */

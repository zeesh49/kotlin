// RUN_PIPELINE_TILL: FRONTEND
val p1: Int = 1 - 1
val p2: Long = 1 - 1
val p3: Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>
val p4: Short = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>

val l1: Long = 1 - 1.toLong()
val l2: Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toLong()<!>
val l3: Int = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toLong()<!>
val l4: Short = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toLong()<!>

val b1: Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toByte()<!>
val b2: Int = 1 - 1.toByte()
val b3: Long = <!TYPE_MISMATCH!>1 - 1.toByte()<!>
val b4: Short = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toByte()<!>

val i1: Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toInt()<!>
val i2: Int = 1 - 1.toInt()
val i3: Long = <!TYPE_MISMATCH!>1 - 1.toInt()<!>
val i4: Short = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toInt()<!>

val s1: Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toShort()<!>
val s2: Int = 1 - 1.toShort()
val s3: Long = <!TYPE_MISMATCH!>1 - 1.toShort()<!>
val s4: Short = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toShort()<!>

/* GENERATED_FIR_TAGS: additiveExpression, integerLiteral, propertyDeclaration */

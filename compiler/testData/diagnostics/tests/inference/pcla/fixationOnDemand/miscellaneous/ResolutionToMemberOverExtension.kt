// RUN_PIPELINE_TILL: BACKEND
fun test() {
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val result = <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.nullaryFunction()
        <!DEBUG_INFO_EXPRESSION_TYPE("ExtensionFunctionResult")!>result<!>
    }
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val result = <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.unaryFunction(SpecificCallArgument())
        <!DEBUG_INFO_EXPRESSION_TYPE("ExtensionFunctionResult")!>result<!>
    }
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val result = <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.unaryFunction(GeneralCallArgument())
        <!DEBUG_INFO_EXPRESSION_TYPE("ExtensionFunctionResult")!>result<!>
    }
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

open class GeneralCallArgument
class SpecificCallArgument: GeneralCallArgument()

object MemberFunctionResult
object ExtensionFunctionResult

class ScopeOwner {
    fun nullaryFunction(): MemberFunctionResult = MemberFunctionResult
    fun unaryFunction(arg: SpecificCallArgument): MemberFunctionResult = MemberFunctionResult
}

fun ScopeOwner.<!EXTENSION_SHADOWED_BY_MEMBER!>nullaryFunction<!>(): ExtensionFunctionResult = ExtensionFunctionResult
fun ScopeOwner.unaryFunction(arg: GeneralCallArgument): ExtensionFunctionResult = ExtensionFunctionResult

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, localProperty, nullableType, objectDeclaration, propertyDeclaration, typeParameter */

// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73392
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun case_4(block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, SampleObject.invocationKind)<!> }
    return block()
}

object SampleObject {
    val invocationKind = InvocationKind.EXACTLY_ONCE
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, functionDeclaration, functionalType, inline, lambdaLiteral,
objectDeclaration, propertyDeclaration */

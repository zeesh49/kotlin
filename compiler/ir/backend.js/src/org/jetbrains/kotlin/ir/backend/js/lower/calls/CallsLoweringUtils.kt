/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.name.Name

typealias SymbolToTransformer = MutableMap<IrFunctionSymbol, (IrFunctionAccessExpression) -> IrExpression>

internal fun SymbolToTransformer.add(from: Map<IrClassifierSymbol, IrFunctionSymbol>, to: IrSimpleFunctionSymbol) {
    from.forEach { _, func ->
        add(func, to)
    }
}

internal fun SymbolToTransformer.add(from: Map<IrClassifierSymbol, IrFunctionSymbol>, to: (IrFunctionAccessExpression) -> IrExpression) {
    from.forEach { _, func ->
        add(func, to)
    }
}

internal fun SymbolToTransformer.add(from: IrFunctionSymbol, to: (IrFunctionAccessExpression) -> IrExpression) {
    put(from, to)
}

internal fun SymbolToTransformer.add(from: IrFunctionSymbol, to: IrSimpleFunctionSymbol) {
    put(from) { call -> irCall(call, to) }
}

internal fun <K> MutableMap<K, (IrFunctionAccessExpression) -> IrExpression>.addWithPredicate(
    from: K,
    predicate: (IrFunctionAccessExpression) -> Boolean,
    action: (IrFunctionAccessExpression) -> IrExpression
) {
    put(from) { call: IrFunctionAccessExpression -> if (predicate(call)) action(call) else call }
}

internal typealias MemberToTransformer = HashMap<SimpleMemberKey, (IrFunctionAccessExpression) -> IrExpression>

internal fun MemberToTransformer.add(type: IrType, name: Name, v: IrSimpleFunctionSymbol) {
    add(type, name) { irCall(it, v) }
}

internal fun MemberToTransformer.add(type: IrType, name: Name, v: IrSimpleFunction) {
    add(type, name, v.symbol)
}

internal fun MemberToTransformer.add(type: IrType, name: Name, v: (IrFunctionAccessExpression) -> IrExpression) {
    put(SimpleMemberKey(type, name), v)
}

internal data class SimpleMemberKey(val klass: IrType, val name: Name)

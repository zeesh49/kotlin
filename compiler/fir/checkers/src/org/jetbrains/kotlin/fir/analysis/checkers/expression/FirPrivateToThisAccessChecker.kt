/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.types.EnrichedProjectionKind
import org.jetbrains.kotlin.types.Variance

object FirPrivateToThisAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val reference = expression.calleeReference.resolved ?: return
        if (reference is FirResolvedErrorReference) {
            // If there was a visibility diagnostic, no need to report another one about visibility
            when (reference.diagnostic) {
                is ConeVisibilityError,
                -> return
            }
        }
        val dispatchReceiver = expression.dispatchReceiver ?: return
        val symbol = reference.toResolvedCallableSymbol(discardErrorReference = true) ?: return
        if (symbol.visibility != Visibilities.Private) return
        val session = context.session
        val containingClassSymbol = symbol.containingClassLookupTag()?.toClassSymbol(session)

        if (!isPrivateToThis(symbol.unwrapFakeOverrides(), containingClassSymbol, session)) return

        val invisible = when (val receiverReference = dispatchReceiver.toReference(session)) {
            is FirThisReference -> receiverReference.boundSymbol != containingClassSymbol
            else -> true
        }

        if (invisible) {
            reporter.reportOn(
                source = expression.source,
                factory = FirErrors.INVISIBLE_REFERENCE,
                a = symbol,
                b = Visibilities.PrivateToThis,
                c = symbol.callableId!!.classId
            )
        }
    }

    private fun isPrivateToThis(
        symbol: FirCallableSymbol<*>,
        containingClassSymbol: FirClassSymbol<*>?,
        session: FirSession
    ): Boolean {
        if (containingClassSymbol == null) return false
        if (symbol is FirConstructorSymbol) return false
        if (containingClassSymbol.typeParameterSymbols.all { it.variance == Variance.INVARIANT }) return false
        // KT-68636 data class generated copy method can never have privateToThis visibility
        // We have to explicitly exclude data class copy because a general case isn't yet supported KT-35396
        if (symbol.isDataClassCopy(containingClassSymbol, session)) return false

        if (symbol.resolvedReceiverType?.contradictsWith(Variance.IN_VARIANCE, session) == true) {
            return true
        }
        if (symbol.resolvedReturnType.contradictsWith(
                if (symbol is FirPropertySymbol && symbol.isVar) Variance.INVARIANT
                else Variance.OUT_VARIANCE,
                session
            )
        ) {
            return true
        }
        if (symbol is FirFunctionSymbol<*>) {
            for (parameter in symbol.valueParameterSymbols) {
                if (parameter.resolvedReturnType.contradictsWith(Variance.IN_VARIANCE, session)) {
                    return true
                }
            }
        }
        for (parameter in symbol.contextParameterSymbols) {
            if (parameter.resolvedReturnType.contradictsWith(Variance.IN_VARIANCE, session)) {
                return true
            }
        }
        return false
    }

    private fun ConeKotlinType.contradictsWith(requiredVariance: Variance, session: FirSession): Boolean {
        when (this) {
            is ConeLookupTagBasedType -> {
                if (this is ConeTypeParameterType) {
                    return !this.lookupTag.typeParameterSymbol.variance.allowsPosition(requiredVariance)
                }
                if (this is ConeClassLikeType) {
                    // It's safe to access fir here, because later we access only variance of type parameters of the class
                    // And variance can not be changed after raw fir stage
                    @OptIn(SymbolInternals::class)
                    val classLike = this.lookupTag.toSymbol(session)?.fir ?: return false
                    for ((index, argument) in this.typeArguments.withIndex()) {
                        val typeParameterRef = classLike.typeParameters.getOrNull(index)
                        if (typeParameterRef !is FirTypeParameter) continue
                        val requiredVarianceForArgument = when (
                            EnrichedProjectionKind.getEffectiveProjectionKind(typeParameterRef.variance, argument.variance)
                        ) {
                            EnrichedProjectionKind.OUT -> requiredVariance
                            EnrichedProjectionKind.IN -> requiredVariance.opposite()
                            EnrichedProjectionKind.INV -> Variance.INVARIANT
                            EnrichedProjectionKind.STAR -> continue // CONFLICTING_PROJECTION error was reported
                        }
                        val argType = argument.type ?: continue
                        if (argType.contradictsWith(requiredVarianceForArgument, session)) {
                            return true
                        }
                    }
                }
            }
            is ConeFlexibleType -> {
                return lowerBound.contradictsWith(requiredVariance, session)
            }
            is ConeDefinitelyNotNullType -> {
                return original.contradictsWith(requiredVariance, session)
            }
            is ConeIntersectionType -> {
                return this.intersectedTypes.any { it.contradictsWith(requiredVariance, session) }
            }
            is ConeCapturedType -> {
                // Looks like not possible here
                return false
            }
            is ConeIntegerConstantOperatorType,
            is ConeIntegerLiteralConstantType,
            is ConeStubTypeForTypeVariableInSubtyping,
            is ConeTypeVariableType,
            -> return false
        }
        return false
    }
}

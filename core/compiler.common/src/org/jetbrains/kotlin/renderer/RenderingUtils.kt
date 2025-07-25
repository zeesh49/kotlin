/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

fun Name.render(): String {
    return if (this.shouldBeEscaped()) '`' + asString() + '`' else asString()
}

private fun Name.shouldBeEscaped(): Boolean {
    val string = asString()
    return string in KeywordStringsGenerated.KEYWORDS ||
            string.any { !Character.isLetterOrDigit(it) && it != '_' } ||
            string.isEmpty() ||
            !Character.isJavaIdentifierStart(string.codePointAt(0))
}

fun FqNameUnsafe.render(): String {
    return renderFqName(pathSegments())
}

fun FqName.render(): String {
    return renderFqName(pathSegments())
}

fun renderFqName(pathSegments: List<Name>): String {
    return buildString {
        for (element in pathSegments) {
            if (length > 0) {
                append(".")
            }
            append(element.render())
        }
    }
}

fun renderFlexibleMutabilityOrArrayElementVarianceType(
    lowerRendered: String,
    upperRendered: String,
    renderKotlinCollectionsPrefix: () -> String,
    renderKotlinPrefix: () -> String,
    escape: (String) -> String = { it },
): String? {
    val kotlinCollectionsPrefix = renderKotlinCollectionsPrefix()
    val mutablePrefix = "Mutable"
    // java.util.List<Foo> -> (Mutable)List<Foo!>!
    val simpleCollection = replacePrefixesInTypeRepresentations(
        lowerRendered,
        kotlinCollectionsPrefix + mutablePrefix,
        upperRendered,
        kotlinCollectionsPrefix,
        "$kotlinCollectionsPrefix($mutablePrefix)"
    )
    if (simpleCollection != null) return simpleCollection
    // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
    val mutableEntry = replacePrefixesInTypeRepresentations(
        lowerRendered,
        kotlinCollectionsPrefix + "MutableMap.MutableEntry",
        upperRendered,
        kotlinCollectionsPrefix + "Map.Entry",
        "$kotlinCollectionsPrefix(Mutable)Map.(Mutable)Entry"
    )
    if (mutableEntry != null) return mutableEntry

    val kotlinPrefix = renderKotlinPrefix()
    // Foo[] -> Array<(out) Foo!>!
    val array = replacePrefixesInTypeRepresentations(
        lowerRendered,
        kotlinPrefix + escape("Array<"),
        upperRendered,
        kotlinPrefix + escape("Array<out "),
        kotlinPrefix + escape("Array<(out) ")
    )
    if (array != null) return array

    return null
}

fun replacePrefixesInTypeRepresentations(
    lowerRendered: String,
    lowerPrefix: String,
    upperRendered: String,
    upperPrefix: String,
    foldedPrefix: String
): String? {
    if (lowerRendered.startsWith(lowerPrefix) && upperRendered.startsWith(upperPrefix)) {
        val lowerWithoutPrefix = lowerRendered.substring(lowerPrefix.length)
        val upperWithoutPrefix = upperRendered.substring(upperPrefix.length)
        val flexibleCollectionName = foldedPrefix + lowerWithoutPrefix

        if (lowerWithoutPrefix == upperWithoutPrefix) return flexibleCollectionName

        if (typeStringsDifferOnlyInNullability(lowerWithoutPrefix, upperWithoutPrefix)) {
            return "$flexibleCollectionName!"
        }
    }
    return null
}

fun typeStringsDifferOnlyInNullability(lower: String, upper: String) =
    lower == upper.replace("?", "") || upper.endsWith("?") && ("$lower?") == upper || "($lower)?" == upper


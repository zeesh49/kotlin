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

package org.jetbrains.kotlin.android.synthetic.idea.res

import com.android.tools.idea.gradle.facet.AndroidGradleFacet
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.AndroidPackageFragmentProviderExtension
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral

class IDEAndroidPackageFragmentProviderExtension : AndroidPackageFragmentProviderExtension() {
    override fun getLayoutXmlFileManager(project: Project, moduleInfo: ModuleInfo?): AndroidLayoutXmlFileManager? {
        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return null
        val module = moduleSourceInfo.module
        if (!isAndroidExtensionsPluginEnabled(module)) return null

        return ModuleServiceManager.getService(module, AndroidLayoutXmlFileManager::class.java)
    }

    private fun isAndroidExtensionsPluginEnabled(module: Module): Boolean {
        val androidGradleFacet = AndroidGradleFacet.getInstance(module) ?: return false
        val vFile = androidGradleFacet.gradleModel?.buildFile ?: return false
        val grFile = PsiManager.getInstance(module.project).findFile(vFile) as? GroovyFile ?: return false

        val savedState = grFile.getUserData(ANDROID_EXTENSIONS_PLUGIN_STATE)
        if (savedState != null && savedState.first == vFile.timeStamp) {
            return savedState.second
        }

        var found = false
        grFile.accept(object : GroovyRecursiveElementVisitor() {
            private var done = false

            override fun visitElement(node: GroovyPsiElement?) {
                if (done) return
                super.visitElement(node)
            }

            override fun visitApplicationStatement(node: GrApplicationStatement) {
                if (node.invokedExpression.text != "apply") return

                val pluginArgument = node.namedArguments.apply { if (size != 1) return }.first()
                if (pluginArgument.labelName == "plugin"
                        && (pluginArgument.expression as? GrLiteral)?.value == "kotlin-android-extensions") {
                    found = true; done = true
                }
            }
        })
        grFile.putUserData(ANDROID_EXTENSIONS_PLUGIN_STATE, Pair(vFile.timeStamp, found))
        return found
    }

    private companion object {
        val ANDROID_EXTENSIONS_PLUGIN_STATE = Key<Pair<Long, Boolean>>("ANDROID_EXTENSIONS_PLUGIN_STATE")
    }
}
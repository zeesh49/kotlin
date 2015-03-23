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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.impl.PositionUtil
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

public abstract class AbstractJetPositionManagerWithDebugProcessTest : KotlinDebuggerTestBase() {
    protected fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        createDebugProcess(path)

        val expectedClassNames = getExpectedClassNames(fileText)

        for (expectedClassName in expectedClassNames) {
            onBreakpoint {
                try {
                    val position = PositionUtil.getSourcePosition(this)

                    runReadAction {
                        val classes = getDebugProcess().getPositionManager().getAllClasses(position)
                        assertNotNull(classes)
                        assertEquals(1, classes.size(), "Couldn't find class for ${position.getLine() + 1}")
                        val type = classes.get(0)
                        assertTrue(type.name().matches(expectedClassName), "Type name ${type.name()} doesn't match $expectedClassName for line ${position.getLine() + 1}")

                    }
                }
                catch(e: Exception) {
                    e.printStackTrace()
                }
                finally {
                    resume(this)
                }
            }
        }

        finish()
    }

    public fun getExpectedClassNames(fileContent: String): List<String> {
        val classNames = arrayListOf<String>()
        val lines = fileContent.split("\n")

        for (i in lines.indices) {
            val lineStr = lines[i].trim()
            if (lineStr == "//Breakpoint!") continue

            val matcher = AbstractJetPositionManagerTest.BREAKPOINT_PATTERN.matcher(lineStr)
            if (matcher.matches()) {
                classNames.add(matcher.group(1))
            }
        }

        return classNames
    }


}

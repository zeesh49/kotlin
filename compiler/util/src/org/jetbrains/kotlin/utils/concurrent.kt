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

package org.jetbrains.kotlin.utils.concurrent.block

import java.io.Serializable

public class LockedClearableLazyValue<T: Any>(val lock: Any, val init: () -> T) {
    @Volatile private var value: T? = null

    public fun get(): T {
        val _v1 = value
        if (_v1 != null) {
            return _v1
        }

        return synchronized(lock) {
            val _v2 = value
            // Suppress because of https://youtrack.jetbrains.com/issue/KT-6176
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            if (_v2 != null) {
                _v2!!
            }
            else {
                val _v3 = init()
                this.value = _v3
                _v3
            }
        }
    }

    fun drop() {
        synchronized (lock) {
            value = null
        }
    }
}

public fun volatileLazy<T>(initializer: () -> T): VolatileLazyImpl<T> = VolatileLazyImpl(initializer)
public fun <T> VolatileLazyImpl<T>.get(thisRef: Any?, property: PropertyMetadata): T = value

private open class VolatileLazyImpl<out T>(initializer: () -> T) : Serializable {
    object UNINITIALIZED_VALUE

    private @Volatile var _initializer: (() -> T)? = initializer
    private @Volatile var _value: Any? = UNINITIALIZED_VALUE

    val value: T get() {
        if (_value !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

        val initializer = _initializer
        if (initializer != null) {
            _value = initializer()
            _initializer = null
        }

        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}
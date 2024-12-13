/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.test.rule

import kotlin.reflect.KClass
import org.junit.runner.Description
import org.junit.runner.JUnitCore
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/**
 * Provides a way to run tests that returns results as a simple list of strings for verification.
 */
class StringingListener : RunListener() {
    private var thisResult: String? = null
    val allResults = mutableListOf<String>()

    override fun testStarted(description: Description?) {
        thisResult = "PASSED"
    }

    override fun testFailure(failure: Failure?) {
        if (failure != null) {
            thisResult = "FAILED: ${failure.message}"
        }
    }

    override fun testAssumptionFailure(failure: Failure?) {
        if (failure != null) {
            thisResult = "SKIPPED: ${failure.message}"
        }
    }

    override fun testFinished(description: Description?) {
        if (description != null) {
            allResults.add("${description.methodName}: $thisResult")
        }
    }

    companion object {
        fun run(klass: KClass<*>): List<String> {
            val listener = StringingListener()
            JUnitCore().apply { addListener(listener) }.run(klass.java)
            return listener.allResults
        }
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.flicker.assertions

import android.tools.FLICKER_TAG
import android.tools.flicker.AssertionInvocationGroup
import android.tools.flicker.subject.exceptions.FlickerAssertionError
import android.tools.io.Reader
import android.tools.withTracing
import android.util.Log
import org.junit.AssumptionViolatedException

// internal data class but visible for testing
data class ScenarioAssertionImpl(
    private val name: String,
    private val reader: Reader,
    private val assertionData: Collection<AssertionData>,
    override val stabilityGroup: AssertionInvocationGroup,
    private val assertionExtraData: Map<String, String>,
    private val assertionRunner: AssertionRunner = ReaderAssertionRunner(reader)
) : ScenarioAssertion {
    init {
        require(assertionData.isNotEmpty()) { "Expected at least one assertion data object." }
    }

    override fun execute(): AssertionResult =
        withTracing("executeAssertion") {
            val assertionExceptions = assertionData.map { assertionRunner.runAssertion(it) }

            val unexpectedExceptions =
                assertionExceptions.filterNot {
                    it == null ||
                        it is FlickerAssertionError ||
                        it is AssumptionViolatedException ||
                        it is IllegalArgumentException
                }
            if (unexpectedExceptions.isNotEmpty()) {
                throw IllegalArgumentException(
                    "Expected all assertion exceptions to be " +
                        "FlickerAssertionErrors or AssumptionViolatedExceptions",
                    unexpectedExceptions.first()
                )
            }

            val assertionErrors = assertionExceptions.filterIsInstance<FlickerAssertionError>()
            assertionErrors.forEach { assertion ->
                assertionExtraData.forEach { (key, value) ->
                    assertion.messageBuilder.addExtraDescription(key, value)
                }
            }

            AssertionResultImpl(
                    name,
                    assertionData,
                    assumptionViolations =
                        assertionExceptions.filterIsInstance<AssumptionViolatedException>(),
                    assertionErrors,
                    stabilityGroup
                )
                .also { log(it) }
        }

    override fun toString() = name

    private fun log(result: AssertionResult) {
        when (result.status) {
            AssertionResult.Status.ASSUMPTION_VIOLATION ->
                Log.w(
                    "$FLICKER_TAG-SERVICE",
                    "${result.name} ASSUMPTION VIOLATION :: " +
                        result.assumptionViolations.map { it.message }
                )
            AssertionResult.Status.PASS -> Log.d("$FLICKER_TAG-SERVICE", "${result.name} PASSED")
            AssertionResult.Status.FAIL ->
                Log.e(
                    "$FLICKER_TAG-SERVICE",
                    "${result.name} FAILED :: " + result.assertionErrors.map { it.message }
                )
        }
    }
}

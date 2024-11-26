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

package android.tools.flicker.assertions

import android.tools.flicker.AssertionInvocationGroup
import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertors.AssertionTemplate
import android.tools.flicker.subject.exceptions.SimpleFlickerAssertionError
import android.tools.io.Reader
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Test
import org.mockito.Mockito

class ScenarioAssertionTest {
    @Test
    fun addsExtraDataToFlickerAssertionMessage() {
        val mockReader = Mockito.mock(Reader::class.java)
        val mockAssertionData = Mockito.mock(AssertionData::class.java)
        val mockAssertionRunner = Mockito.mock(AssertionRunner::class.java)

        val scenarioAssertion =
            ScenarioAssertionImpl(
                name = "My Assertion",
                reader = mockReader,
                assertionData = listOf(mockAssertionData),
                stabilityGroup = AssertionInvocationGroup.BLOCKING,
                assertionExtraData = mapOf("extraKey" to "extraValue"),
                assertionRunner = mockAssertionRunner,
            )

        Mockito.`when`(mockAssertionRunner.runAssertion(mockAssertionData))
            .thenReturn(SimpleFlickerAssertionError("My assertion"))

        val assertionResult = scenarioAssertion.execute()

        Truth.assertThat(assertionResult.assertionErrors).hasSize(1)
        val assertionMessage = assertionResult.assertionErrors.first().message
        Truth.assertThat(assertionMessage).contains("My assertion")
        Truth.assertThat(assertionMessage).contains("extraKey")
        Truth.assertThat(assertionMessage).contains("extraValue")
    }

    @Test
    fun supportsRunningAssertionsWithTruthAssertions() {
        val mockReader = Mockito.mock(Reader::class.java)
        val mockScenarioInstance = Mockito.mock(ScenarioInstance::class.java)

        val assertionTemplate =
            object : AssertionTemplate("MyCustomAssertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    Truth.assertThat("abc").isEqualTo("efg")
                }
            }

        val scenarioAssertion =
            ScenarioAssertionImpl(
                name = "My Assertion",
                reader = mockReader,
                assertionData = assertionTemplate.createAssertions(mockScenarioInstance),
                stabilityGroup = AssertionInvocationGroup.BLOCKING,
                assertionExtraData = mapOf(),
            )

        val result = scenarioAssertion.execute()

        Truth.assertThat(result.status).isEqualTo(AssertionResult.Status.FAIL)
        Truth.assertThat(result.assertionErrors).hasSize(1)
        Truth.assertThat(result.assertionErrors.first())
            .hasMessageThat()
            .contains("expected: efg\nbut was : abc")
    }

    @Test
    fun supportsRunningAssertionsWithAssumptionViolations() {
        val mockReader = Mockito.mock(Reader::class.java)
        val mockScenarioInstance = Mockito.mock(ScenarioInstance::class.java)

        val assertionTemplate =
            object : AssertionTemplate("MyCustomAssertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    Assume.assumeTrue(false)
                }
            }

        val scenarioAssertion =
            ScenarioAssertionImpl(
                name = "My Assertion",
                reader = mockReader,
                assertionData = assertionTemplate.createAssertions(mockScenarioInstance),
                stabilityGroup = AssertionInvocationGroup.BLOCKING,
                assertionExtraData = mapOf(),
            )

        val result = scenarioAssertion.execute()

        Truth.assertThat(result.status).isEqualTo(AssertionResult.Status.ASSUMPTION_VIOLATION)
        Truth.assertThat(result.assumptionViolations).hasSize(1)
        Truth.assertThat(result.assumptionViolations.first())
            .hasMessageThat()
            .isEqualTo("got: <false>, expected: is <true>")
    }
}

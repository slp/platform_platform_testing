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

package platform.test.motion.truth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Correspondence
import com.google.common.truth.ExpectFailure
import com.google.common.truth.TruthFailureSubject
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.Feature
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimestampFrameId
import platform.test.motion.golden.asDataPoint
import platform.test.motion.truth.TimeSeriesSubject.Companion.assertThat

@RunWith(AndroidJUnit4::class)
class TimeSeriesSubjectTest {

    @Test
    fun isEqualTo_nonTimeSeriesObject_usesDefaultImplementation() {
        with(assertThrows { assertThat(TimeSeries(listOf(), emptyList())).isEqualTo("foo") }) {
            factValue("expected").isEqualTo("foo")
            factValue("but was").isEqualTo("TimeSeries(frameIds=[], features={})")
        }
    }

    @Test
    fun isEqualTo_matchingTimeSeries() {
        val timeSeries = createTimeSeries(2)

        assertThat(timeSeries).isEqualTo(timeSeries.copy())
    }

    @Test
    fun isEqualTo_actualHasDifferentFrameTimes() {
        val expected = createTimeSeries(2)
        val actual =
            expected.copy(frameIds = listOf(expected.frameIds[0], SupplementalFrameId("x")))

        with(assertThrows { assertThat(actual).isEqualTo(expected) }) {
            factKeys().contains("TimeSeries.frames does not match")
            factValue("|  expected").isEqualTo("[0ms, 1ms]")
            factValue("|  but got").isEqualTo("[0ms, x]")
            factValue("|  unexpected (1)").isEqualTo("[x]")
            factValue("|  missing (1)").isEqualTo("[1ms]")

            factKeys().comparingElementsUsing(startsWith).doesNotContain("TimeSeries.features")
        }
    }

    @Test
    fun isEqualTo_missingFrame() {
        val expected = createTimeSeries(3)
        val actual = createTimeSeries(2)

        with(assertThrows { assertThat(actual).isEqualTo(expected) }) {
            factKeys().contains("TimeSeries.frames does not match")

            factValue("|  expected").isEqualTo("[0ms, 1ms, 2ms]")
            factValue("|  but got").isEqualTo("[0ms, 1ms]")
            factValue("|  missing (1)").isEqualTo("[2ms]")

            factKeys().comparingElementsUsing(startsWith).doesNotContain("TimeSeries.features")
        }
    }

    @Test
    fun isEqualTo_additionalFrame() {
        val expected = createTimeSeries(1)
        val actual = createTimeSeries(2)

        with(assertThrows { assertThat(actual).isEqualTo(expected) }) {
            factKeys().contains("TimeSeries.frames does not match")

            factValue("|  expected").isEqualTo("[0ms]")
            factValue("|  but got").isEqualTo("[0ms, 1ms]")
            factValue("|  unexpected (1)").isEqualTo("[1ms]")

            factKeys().comparingElementsUsing(startsWith).doesNotContain("TimeSeries.features")
        }
    }

    @Test
    fun isEqualTo_missingFeature() {
        val expected = createTimeSeries(2)
        val actual =
            expected.copy(
                features =
                    buildMap {
                        putAll(expected.features)
                        remove("bar")
                    }
            )

        with(assertThrows { assertThat(actual).isEqualTo(expected) }) {
            factKeys().contains("TimeSeries.features does not match")

            factValue("|  missing (1)").isEqualTo("[bar]")
        }
    }

    @Test
    fun isEqualTo_additionalFeature() {
        val expected = createTimeSeries(2)
        val actual =
            expected.copy(
                features =
                    buildMap {
                        putAll(expected.features)
                        put("baz", createIntFeature("baz", 2))
                    }
            )

        with(assertThrows { assertThat(actual).isEqualTo(expected) }) {
            factKeys().contains("TimeSeries.features does not match")

            factValue("|  unexpected (1)").isEqualTo("[baz]")
        }
    }

    @Test
    fun isEqualTo_actualHasDifferentDataPoint() {
        val expected =
            TimeSeries(
                createFrames(2),
                listOf(Feature("foo", listOf(1.asDataPoint(), 2.asDataPoint()))),
            )
        val actual =
            TimeSeries(
                createFrames(2),
                listOf(Feature("foo", listOf(1.asDataPoint(), 3.asDataPoint()))),
            )

        with(assertThrows { assertThat(actual).isEqualTo(expected) }) {
            factKeys()
                .containsExactly(
                    "TimeSeries.features[foo].dataPoints do not match",
                    "|  @1ms",
                    "|    expected",
                    "|    but was",
                    TimeSeriesSubject.MANAGE_GOLDEN_DOCUMENTATION,
                )
                .inOrder()

            factValue("|    expected").isEqualTo("2 (int)")
            factValue("|    but was").isEqualTo("3 (int)")
        }
    }

    @Test
    fun isEqualTo_manageGoldenMessageAddedOnError() {
        val expected =
            TimeSeries(
                createFrames(2),
                listOf(Feature("foo", listOf(1.asDataPoint(), 2.asDataPoint()))),
            )
        val actual =
            TimeSeries(
                createFrames(2),
                listOf(Feature("foo", listOf(1.asDataPoint(), 3.asDataPoint()))),
            )

        with(assertThrows { assertThat(actual).isEqualTo(expected) }) {
            factKeys().contains(TimeSeriesSubject.MANAGE_GOLDEN_DOCUMENTATION)
        }
    }

    private fun createTimeSeries(frameCount: Int) =
        TimeSeries(
            createFrames(frameCount),
            listOf(createIntFeature("foo", frameCount), createStringFeature("bar", frameCount)),
        )

    private fun createFrames(frameCount: Int) = List(frameCount) { TimestampFrameId(it.toLong()) }

    private fun createIntFeature(name: String, dataPoints: Int) =
        Feature(name, List(dataPoints) { it.asDataPoint() })

    private fun createStringFeature(name: String, dataPoints: Int) =
        Feature(name, List(dataPoints) { it.toString().asDataPoint() })

    private inline fun assertThrows(body: () -> Unit): TruthFailureSubject {
        try {
            body()
        } catch (e: Throwable) {
            if (e is AssertionError) {
                return ExpectFailure.assertThat(e)
            }
            throw e
        }
        throw AssertionError("Body completed successfully. Expected AssertionError")
    }

    companion object {
        val startsWith: Correspondence<String, String> =
            Correspondence.from(
                { actual, expected -> checkNotNull(actual).startsWith(checkNotNull(expected)) },
                "starts with",
            )
    }
}

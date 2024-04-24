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

import com.google.common.truth.Fact
import com.google.common.truth.Fact.fact
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import platform.test.motion.golden.TimeSeries

/** Subject on [TimeSeries] to produce meaningful failure diffs. */
class TimeSeriesSubject
private constructor(failureMetadata: FailureMetadata, private val actual: TimeSeries?) :
    Subject(failureMetadata, actual) {

    override fun isEqualTo(expected: Any?) {
        if (actual is TimeSeries && expected is TimeSeries) {
            val facts = compareTimeSeries(expected, actual)
            if (facts.isNotEmpty()) {
                failWithoutActual(facts[0], *(facts.drop(1)).toTypedArray())
            }
        } else {
            super.isEqualTo(expected)
        }
    }

    private fun compareTimeSeries(expected: TimeSeries, actual: TimeSeries) =
        buildList<Fact> {
            val actualToExpectedDataPointIndices: List<Pair<Int, Int>>
            if (actual.frameIds != expected.frameIds) {
                add(simpleFact("TimeSeries.frames does not match"))
                add(fact("|  expected", expected.frameIds.map { it.label }))
                add(fact("|  but got", actual.frameIds.map { it.label }))

                val actualFrameIds = actual.frameIds.toSet()
                val expectedFrameIds = expected.frameIds.toSet()
                val framesToCompare = actualFrameIds.intersect(expectedFrameIds)

                if (framesToCompare != actualFrameIds) {
                    val unexpected = actualFrameIds - framesToCompare
                    add(fact("|  unexpected (${ unexpected.size})", unexpected.map { it.label }))
                }

                if (framesToCompare != expectedFrameIds) {
                    val missing = expectedFrameIds - framesToCompare
                    add(fact("|  missing (${ missing.size})", missing.map { it.label }))
                }
                actualToExpectedDataPointIndices =
                    framesToCompare.map {
                        actual.frameIds.indexOf(it) to expected.frameIds.indexOf(it)
                    }
            } else {
                actualToExpectedDataPointIndices = List(actual.frameIds.size) { it to it }
            }

            val featuresToCompare: Set<String>
            if (actual.features.keys != expected.features.keys) {
                featuresToCompare = actual.features.keys.intersect(expected.features.keys)
                add(simpleFact("TimeSeries.features does not match"))

                if (featuresToCompare != actual.features.keys) {
                    val unexpected = actual.features.keys - featuresToCompare
                    add(fact("|  unexpected (${ unexpected.size})", unexpected))
                }

                if (featuresToCompare != expected.features.keys) {
                    val missing = expected.features.keys - featuresToCompare
                    add(fact("|  missing (${ missing.size})", missing))
                }
            } else {
                featuresToCompare = actual.features.keys
            }

            featuresToCompare.forEach { featureKey ->
                val actualFeature = checkNotNull(actual.features[featureKey])
                val expectedFeature = checkNotNull(expected.features[featureKey])

                val mismatchingDataPointIndices =
                    actualToExpectedDataPointIndices.filter { (actualIndex, expectedIndex) ->
                        actualFeature.dataPoints[actualIndex] !=
                            expectedFeature.dataPoints[expectedIndex]
                    }

                if (mismatchingDataPointIndices.isNotEmpty()) {
                    add(simpleFact("TimeSeries.features[$featureKey].dataPoints do not match"))

                    mismatchingDataPointIndices.forEach { (actualIndex, expectedIndex) ->
                        add(simpleFact("|  @${actual.frameIds[actualIndex].label}"))
                        add(fact("|    expected", expectedFeature.dataPoints[expectedIndex]))
                        add(fact("|    but was", actualFeature.dataPoints[actualIndex]))
                    }
                }
            }
        }

    companion object {
        /** Returns a factory to be used with [Truth.assertAbout]. */
        fun timeSeries(): Factory<TimeSeriesSubject, TimeSeries> {
            return Factory { failureMetadata: FailureMetadata, subject: TimeSeries? ->
                TimeSeriesSubject(failureMetadata, subject)
            }
        }

        /** Shortcut for `Truth.assertAbout(timeSeries()).that(timeSeries)`. */
        fun assertThat(timeSeries: TimeSeries): TimeSeriesSubject =
            Truth.assertAbout(timeSeries()).that(timeSeries)
    }
}

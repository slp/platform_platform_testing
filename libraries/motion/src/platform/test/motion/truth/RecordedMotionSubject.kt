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

import com.google.common.truth.Fact.fact
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import org.json.JSONException
import platform.test.motion.GoldenNotFoundException
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.truth.TimeSeriesSubject.Companion.timeSeries
import platform.test.screenshot.matchers.BitmapMatcher
import platform.test.screenshot.matchers.PixelPerfectMatcher

/**
 * Subject to verify a [RecordedMotion] against golden data.
 *
 * @see [MotionTestRule.motion]
 */
class RecordedMotionSubject
internal constructor(
    failureMetadata: FailureMetadata,
    private val actual: RecordedMotion?,
    private val motionTestRule: MotionTestRule,
) : Subject(failureMetadata, actual) {

    /**
     * Verifies a time series matches a previously captured golden.
     *
     * @param goldenName the name for the golden. When `null`, the test method name is used.
     */
    fun timeSeriesMatchesGolden(goldenName: String? = null) {
        isNotNull()
        val recordedMotion = checkNotNull(actual)

        val goldenIdentifier = getGoldenIdentifier(recordedMotion, goldenName)
        val actualTimeSeries = recordedMotion.timeSeries

        // Export the actual values, so that they can later be downloaded to update the golden.
        motionTestRule.writeGeneratedTimeSeries(goldenIdentifier, actualTimeSeries)

        var matchesGolden = false
        try {
            try {
                val goldenTimeSeries = motionTestRule.readGoldenTimeSeries(goldenIdentifier)

                check("Motion time-series $goldenIdentifier")
                    .about(timeSeries())
                    .that(actualTimeSeries)
                    .isEqualTo(goldenTimeSeries)

                matchesGolden = true
            } catch (e: GoldenNotFoundException) {
                failWithoutActual(simpleFact("Golden [${e.missingGoldenFile}] not found"))
            } catch (e: JSONException) {
                failWithoutActual(fact("Golden [$goldenIdentifier] file is invalid", e))
            }
        } finally {
            motionTestRule.writeDebugFilmstrip(recordedMotion, goldenIdentifier, matchesGolden)
        }
    }

    /**
     * Verifies that the filmstrip of the recorded motion matches a golden bitmap thereof.
     *
     * Prefer capturing explicit signals and asserting those (via [timeSeriesMatchesGolden]). A
     * filmstrip can easily assert on many irrelevant details that should be tested elsewhere, and
     * could cause the test to fail on many irrelevant changes.
     *
     * @param goldenName the name for the golden. When `null`, the test method name is used.
     */
    fun filmstripMatchesGolden(
        goldenName: String? = null,
        bitmapMatcher: BitmapMatcher = PixelPerfectMatcher()
    ) {
        isNotNull()
        val recordedMotion = checkNotNull(actual)
        val bitmapDiffer =
            checkNotNull(motionTestRule.bitmapDiffer) {
                "BitmapDiffer must be supplied to MotionTestRule for filmstrip golden support"
            }
        val goldenIdentifier = getGoldenIdentifier(recordedMotion, goldenName)
        val filmstrip = recordedMotion.filmstrip.renderFilmstrip()
        bitmapDiffer.assertBitmapAgainstGolden(filmstrip, goldenIdentifier, bitmapMatcher)
    }

    private fun getGoldenIdentifier(recordedMotion: RecordedMotion, goldenName: String?): String =
        goldenName ?: recordedMotion.testMethodName
}

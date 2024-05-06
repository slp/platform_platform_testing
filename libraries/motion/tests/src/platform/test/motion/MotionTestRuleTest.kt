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

package platform.test.motion

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.TimeSeries
import platform.test.motion.testing.JsonSubject.Companion.assertThat
import platform.test.motion.testing.createGoldenPathManager

@RunWith(AndroidJUnit4::class)
class MotionTestRuleTest {

    private val goldenPathManager = createGoldenPathManager("assets")

    private val subject = MotionTestRule(Unit, goldenPathManager)

    private val emptyRecordedMotion =
        RecordedMotion(
            "FooClass",
            "bar_test",
            TimeSeries(listOf(), emptyList()),
            screenshots = null
        )

    @Test
    fun readGoldenTimeSeries_withExistingGolden_returnsParsedJson() {
        assertThat(subject.readGoldenTimeSeries("empty_timeseries", emptyMap()))
            .isEqualTo(TimeSeries(listOf(), emptyList()))
    }

    @Test
    fun readGoldenTimeSeries_withUnavailableGolden_throwsGoldenNotFoundException() {
        val exception =
            assertThrows(GoldenNotFoundException::class.java) {
                subject.readGoldenTimeSeries("no_golden", emptyMap())
            }
        assertThat(exception.missingGoldenFile).endsWith("no_golden.json")
    }

    @Test
    fun readGoldenTimeSeries_withInvalidJsonFile_throwsJSONException() {
        assertThrows(JSONException::class.java) {
            subject.readGoldenTimeSeries("invalid_json_data", emptyMap())
        }
    }

    @Test
    fun writeGeneratedTimeSeries_createsFile() {
        subject.writeGeneratedTimeSeries(
            "updated_golden",
            emptyRecordedMotion,
            TimeSeriesVerificationResult.PASSED
        )
        val expectedFile =
            File(goldenPathManager.deviceLocalPath).resolve("FooClass/updated_golden.json")

        assertThat(expectedFile.exists()).isTrue()
    }

    @Test
    fun writeGeneratedTimeSeries_writesTimeSeries() {
        subject.writeGeneratedTimeSeries(
            "updated_golden",
            emptyRecordedMotion,
            TimeSeriesVerificationResult.PASSED
        )
        val expectedFile =
            File(goldenPathManager.deviceLocalPath).resolve("FooClass/updated_golden.json")

        val fileContentsWithoutMetadata =
            JSONObject(expectedFile.readText()).apply { remove("//metadata") }

        assertThat(fileContentsWithoutMetadata)
            .isEqualTo(
                JSONObject().apply {
                    put("frame_ids", JSONArray())
                    put("features", JSONArray())
                }
            )
    }

    @Test
    fun writeGeneratedTimeSeries_includesMetadata() {
        subject.writeGeneratedTimeSeries(
            "updated_golden",
            emptyRecordedMotion,
            TimeSeriesVerificationResult.MISSING_REFERENCE
        )
        val expectedFile =
            File(goldenPathManager.deviceLocalPath).resolve("FooClass/updated_golden.json")

        val fileContents = JSONObject(expectedFile.readText())

        assertThat(fileContents.get("//metadata") as JSONObject)
            .isEqualTo(
                JSONObject().apply {
                    put("goldenRepoPath", "assets/updated_golden.json")
                    put("filmstripTestIdentifier", "motion_debug_filmstrip_FooClass")
                    put("goldenIdentifier", "updated_golden")
                    put("result", "MISSING_REFERENCE")
                }
            )
    }

    @Test
    fun writeGeneratedTimeSeries_withInvalidIdentifier_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            subject.writeGeneratedTimeSeries(
                "invalid identifier!",
                emptyRecordedMotion,
                TimeSeriesVerificationResult.PASSED
            )
        }
    }
}

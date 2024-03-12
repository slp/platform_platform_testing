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
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.TimeSeries
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.PathConfig

@RunWith(AndroidJUnit4::class)
class MotionTestRuleTest {

    private val goldenPathManager =
        GoldenPathManager(
            InstrumentationRegistry.getInstrumentation().context,
            pathConfig = PathConfig()
        )

    private val subject = MotionTestRule(goldenPathManager)

    @Test
    fun readGoldenTimeSeries_withExistingGolden_returnsParsedJson() {
        assertThat(subject.readGoldenTimeSeries("empty_timeseries"))
            .isEqualTo(TimeSeries(listOf(), emptyList()))
    }

    @Test
    fun readGoldenTimeSeries_withUnavailableGolden_throwsGoldenNotFoundException() {
        val exception =
            assertThrows(GoldenNotFoundException::class.java) {
                subject.readGoldenTimeSeries("no_golden")
            }
        assertThat(exception.missingGoldenFile).endsWith("no_golden.json")
    }

    @Test
    fun readGoldenTimeSeries_withInvalidJsonFile_throwsJSONException() {
        assertThrows(JSONException::class.java) {
            subject.readGoldenTimeSeries("invalid_json_data")
        }
    }

    @Test
    fun writeGeneratedTimeSeries_createsFile() {
        val emptyTimeSeries =
            JSONObject().apply {
                put("frame_ids", JSONArray())
                put("features", JSONArray())
            }
        subject.writeGeneratedTimeSeries("updated_golden", TimeSeries(listOf(), emptyList()))

        val expectedFile = File(goldenPathManager.deviceLocalPath).resolve("updated_golden.json")

        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.readText()).isEqualTo(emptyTimeSeries.toString(2))
    }

    @Test
    fun writeGeneratedTimeSeries_withInvalidIdentifier_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            subject.writeGeneratedTimeSeries(
                "invalid identifier!",
                TimeSeries(listOf(), emptyList())
            )
        }
    }
}

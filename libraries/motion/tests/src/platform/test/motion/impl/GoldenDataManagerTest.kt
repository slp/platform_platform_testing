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

package platform.test.motion.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.JsonSubject.Companion.assertThat
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.PathConfig

@RunWith(AndroidJUnit4::class)
class GoldenDataManagerTest {

    private val goldenPathManager =
        GoldenPathManager(
            InstrumentationRegistry.getInstrumentation().context,
            pathConfig = PathConfig()
        )

    private val subject = GoldenDataManager(goldenPathManager)

    @Test
    fun readGoldenJson_withExistingGolden_returnsParsedJson() {
        assertThat(subject.readGoldenJson("golden_json_data"))
            .isEqualTo(JSONObject().apply { put("valid", "json") })
    }

    @Test
    fun readGoldenJson_withUnavailableGolden_throwsGoldenNotFoundException() {
        val exception =
            assertThrows(GoldenNotFoundException::class.java) {
                subject.readGoldenJson("no_golden")
            }
        assertThat(exception.missingGoldenFile).endsWith("no_golden.json")
    }

    @Test
    fun readGoldenJson_withInvalidJsonFile_throwsJSONException() {
        assertThrows(JSONException::class.java) { subject.readGoldenJson("invalid_json_data") }
    }

    @Test
    fun writeGeneratedJson_createsFile() {
        val sampleJson = JSONObject().apply { put("key", "value") }
        subject.writeGeneratedJson("updated_golden", sampleJson)

        val expectedFile = File(goldenPathManager.deviceLocalPath).resolve("updated_golden.json")

        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.readText()).isEqualTo(sampleJson.toString(2))
    }

    @Test
    fun writeGeneratedJson_withInvalidIdentifier_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            subject.writeGeneratedJson(
                "invalid identifier!",
                JSONObject().apply { put("valid", "json") }
            )
        }
    }
}

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

package platform.test.motion.compose

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.compose.DataPointTypes.dp
import platform.test.motion.compose.DataPointTypes.dpOffset
import platform.test.motion.compose.DataPointTypes.dpSize
import platform.test.motion.compose.DataPointTypes.intSize
import platform.test.motion.testing.DataPointTypeSubject.Companion.assertThat

@RunWith(AndroidJUnit4::class)
class DataPointTypesTest {

    @Test
    fun dp_jsonConversion() {
        assertThat(dp.fromJson(1)).isEqualTo(1.dp.asDataPoint())
        assertThat(1.dp.asDataPoint().asJson()).isEqualTo(1)
        assertThat(dp).invalidJsonReturnsUnknownDataPoint(JSONObject(), "foo")
    }

    @Test
    fun intSize_jsonConversion() {
        assertThat(intSize)
            .convertsJson(IntSize(width = 1, height = 2), """{"width":1, "height": 2}""")

        assertThat(intSize).invalidJsonReturnsUnknownDataPoint(JSONObject(), 1)
    }
    @Test
    fun dpSize_jsonConversion() {
        assertThat(dpSize)
            .convertsJson(DpSize(width = 1.dp, height = 2.dp), """{"width":1, "height": 2}""")

        assertThat(intSize).invalidJsonReturnsUnknownDataPoint(JSONObject(), 1)
    }
    @Test
    fun dpOffset_jsonConversion() {
        assertThat(dpOffset).convertsJson(DpOffset(x = 1.dp, y = 2.dp), """{"x":1, "y": 2}""")

        assertThat(dpOffset).invalidJsonReturnsUnknownDataPoint(JSONObject(), 1)
    }
}

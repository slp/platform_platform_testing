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

package platform.test.motion.testing

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertWithMessage
import org.json.JSONArray
import org.json.JSONObject
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.DataPointType

/** [Truth] subject for testing [DataPointType] implementation. */
class DataPointTypeSubject<T>
private constructor(failureMetadata: FailureMetadata, private val actual: DataPointType<T>?) :
    Subject(failureMetadata, actual) {

    /**
     * Asserts that the [nativeObject] is serialized to/from the object [jsonRepresentation].
     *
     * The [jsonRepresentation] must be a valid JSON object, that means the string is expected to be
     * wrapped in `{` and `}`.
     */
    fun convertsJsonObject(nativeObject: T, jsonRepresentation: String) {
        convertsJson(nativeObject, jsonRepresentation) { JSONObject(it) }
    }

    /**
     * Asserts that the [nativeObject] is serialized to/from the array [jsonRepresentation].
     *
     * The [jsonRepresentation] must be a valid JSON array, that means the string is expected to be
     * wrapped in `[` and `]`.
     */
    fun convertsJsonArray(nativeObject: T, jsonRepresentation: String) {
        convertsJson(nativeObject, jsonRepresentation) { JSONArray(it) }
    }

    private fun convertsJson(
        nativeObject: T,
        jsonRepresentation: String,
        parseJsonRepresentation: (String) -> Any
    ) {
        isNotNull()
        val dataPointType = checkNotNull(actual)

        assertWithMessage("serialize to JSON")
            .about(JsonSubject.json())
            .that(dataPointType.toJson(nativeObject))
            .isEqualTo(parseJsonRepresentation(jsonRepresentation))
        assertWithMessage("deserialize from JSON")
            .that(dataPointType.fromJson(parseJsonRepresentation(jsonRepresentation)))
            .isEqualTo(DataPoint.of(nativeObject, dataPointType))
    }

    fun invalidJsonReturnsUnknownDataPoint(vararg samples: Any) {
        isNotNull()
        val dataPointType = checkNotNull(actual)

        samples.forEach {
            Truth.assertThat(dataPointType.fromJson(JSONObject()))
                .isEqualTo(DataPoint.unknownType<T>())
        }
    }

    companion object {
        /** Returns a factory to be used with [Truth.assertAbout]. */
        fun <T> dataPointType(): Factory<DataPointTypeSubject<T>, DataPointType<T>> {
            return Factory { failureMetadata: FailureMetadata, subject: DataPointType<T>? ->
                DataPointTypeSubject(failureMetadata, subject)
            }
        }

        /** Shortcut for `Truth.assertAbout(dataPointType()).that(dataPointType)`. */
        fun <T> assertThat(dataPointType: DataPointType<T>): DataPointTypeSubject<T> =
            Truth.assertAbout(dataPointType<T>()).that(dataPointType)
    }
}

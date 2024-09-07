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
import org.json.JSONObject
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.ValueDataPoint

/** [Truth] subject for verifying [DataPoint]s. */
class DataPointSubject<T>
private constructor(failureMetadata: FailureMetadata, private val actual: DataPoint<T>?) :
    Subject(failureMetadata, actual) {

    fun isUnknown() {
        check("isUnknown").that(actual).isEqualTo(DataPoint.unknownType<T>())
    }

    fun isNotFound() {
        check("isNotFound").that(actual).isEqualTo(DataPoint.notFound<T>())
    }

    fun hasNativeValue(nativeValue: T) {
        isNotNull()

        check("hasNativeValue").that(actual).isInstanceOf(ValueDataPoint::class.java)
        val valueDataPoint = actual as ValueDataPoint<T>

        assertWithMessage("hasNativeValue").that(valueDataPoint.value).isEqualTo(nativeValue)
    }

    fun hasJsonValue(jsonRepresentation: String) {
        isNotNull()

        assertWithMessage("hasJsonValue")
            .about(JsonSubject.json())
            .that(checkNotNull(actual).asJson())
            .isEqualTo(JSONObject(jsonRepresentation))
    }

    companion object {
        /** Returns a factory to be used with [Truth.assertAbout]. */
        fun <T> dataPoint(): Factory<DataPointSubject<T>, DataPoint<T>> {
            return Factory { failureMetadata: FailureMetadata, subject: DataPoint<T>? ->
                DataPointSubject(failureMetadata, subject)
            }
        }

        /** Shortcut for `Truth.assertAbout(dataPointType()).that(dataPointType)`. */
        fun <T> assertThat(dataPoint: DataPoint<T>): DataPointSubject<T> =
            Truth.assertAbout(dataPoint<T>()).that(dataPoint)
    }
}

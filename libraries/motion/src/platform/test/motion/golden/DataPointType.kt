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

package platform.test.motion.golden

import org.json.JSONException

/**
 * Golden value type to convert to/from JSON.
 *
 * @param typeName identifier written to the JSON, to support de-serialization of the values.
 * @param jsonToValue convert the [jsonValue] to a native [T], throws [JSONException] if conversion
 *   fails.
 * @param valueToJson converts the native [T] to a `org.json` supported type.
 * @param ensureImmutable copies mutable objects, to avoid subsequent modification.
 */
class DataPointType<T>(
    val typeName: String,
    private val jsonToValue: (jsonValue: Any) -> T,
    private val valueToJson: (T) -> Any,
    internal val ensureImmutable: (T & Any) -> T & Any = { it },
) {
    fun makeDataPoint(nativeValue: T?): DataPoint<T> {
        return DataPoint.of(nativeValue, this)
    }

    fun fromJson(jsonValue: Any): DataPoint<T> {
        return when {
            NullDataPoint.isNullValue(jsonValue) -> DataPoint.nullValue()
            NotFoundDataPoint.isNotFoundValue(jsonValue) -> DataPoint.notFound()
            else ->
                try {
                    makeDataPoint(jsonToValue(jsonValue))
                } catch (e: JSONException) {
                    DataPoint.unknownType()
                }
        }
    }

    fun toJson(value: T): Any = valueToJson(value)

    override fun toString(): String {
        return typeName
    }
}

/** Signals that a JSON value cannot be deserialized by a [DataPointType]. */
class UnknownTypeException : JSONException("JSON cannot be converted to DataPoint value")

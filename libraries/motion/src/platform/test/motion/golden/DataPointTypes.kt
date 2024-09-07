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

import org.json.JSONArray

fun Float.asDataPoint() = DataPointTypes.float.makeDataPoint(this)

fun Boolean.asDataPoint() = DataPointTypes.boolean.makeDataPoint(this)

fun Int.asDataPoint() = DataPointTypes.int.makeDataPoint(this)

fun String.asDataPoint() = DataPointTypes.string.makeDataPoint(this)

/** [DataPointType] implementations for core Kotlin types. */
object DataPointTypes {

    val boolean: DataPointType<Boolean> =
        DataPointType(
            "boolean",
            jsonToValue = {
                when {
                    it is Boolean -> it
                    it is String && "true".equals(it, ignoreCase = true) -> true
                    it is String && "false".equals(it, ignoreCase = true) -> false
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = { it }
        )

    val float: DataPointType<Float> =
        DataPointType(
            "float",
            jsonToValue = {
                when (it) {
                    is Float -> it
                    is Number -> it.toFloat()
                    is String -> it.toFloatOrNull() ?: throw UnknownTypeException()
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = { it }
        )

    val int: DataPointType<Int> =
        DataPointType(
            "int",
            jsonToValue = {
                when (it) {
                    is Int -> it
                    is Number -> it.toInt()
                    is String -> it.toIntOrNull() ?: throw UnknownTypeException()
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = { it }
        )

    val string: DataPointType<String> =
        DataPointType("string", jsonToValue = { it.toString() }, valueToJson = { it })

    /**
     * Creates a [DataPointType] to serialize a list of values in an array, using [dataPointType].
     */
    fun <T : Any> listOf(dataPointType: DataPointType<T>): DataPointType<List<T>> {
        return DataPointType(
            "${dataPointType.typeName}[]",
            jsonToValue = {
                when (it) {
                    is JSONArray ->
                        List(it.length()) { index ->
                            val dataPoint = dataPointType.fromJson(it.get(index))
                            if (dataPoint !is ValueDataPoint<T>) {
                                throw UnknownTypeException()
                            }
                            dataPoint.value
                        }
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = {
                JSONArray().apply { it.forEach { value -> put(dataPointType.toJson(value)) } }
            }
        )
    }
}

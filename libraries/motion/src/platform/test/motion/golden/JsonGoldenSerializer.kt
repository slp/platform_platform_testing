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
import org.json.JSONException
import org.json.JSONObject

/**
 * Utility to (de-)serialize golden [TimeSeries] data in a JSON text format.
 *
 * The JSON format is written with human readability in mind.
 *
 * Note that this intentionally does not use protocol buffers, since the text format is not
 * available for the "Protobuf Java Lite Runtime". See http://shortn/_dx5ldOga8s for details.
 */
object JsonGoldenSerializer {
    /**
     * Reads a previously JSON serialized [TimeSeries] data.
     *
     * Golden data types not included in the `typeRegistry` will produce an [UnknownType].
     *
     * @param typeRegistry [DataPointType] implementations used to de-serialize structured JSON
     *   values to golden values. See [TimeSeries.createTypeRegistry] for creating the registry
     *   based on the currently produced timeseries.
     * @throws JSONException if the JSON data does not match the expected schema.
     */
    fun fromJson(jsonObject: JSONObject, typeRegistry: Map<String, DataPointType<*>>): TimeSeries {
        val frameIds =
            jsonObject.getJSONArray(KEY_FRAME_IDS).convert(JSONArray::get, ::frameIdFromJson)

        val features =
            jsonObject.getJSONArray(KEY_FEATURES).convert(JSONArray::getJSONObject) {
                featureFromJson(it, typeRegistry)
            }

        return TimeSeries(frameIds, features)
    }

    /** Creates a [JSONObject] representing the [golden]. */
    fun toJson(golden: TimeSeries) =
        JSONObject().apply {
            put(
                KEY_FRAME_IDS,
                JSONArray().apply { golden.frameIds.map(::frameIdToJson).forEach(this::put) },
            )
            put(
                KEY_FEATURES,
                JSONArray().apply { golden.features.values.map(::featureToJson).forEach(this::put) },
            )
        }

    private fun frameIdFromJson(jsonValue: Any): FrameId {
        return when (jsonValue) {
            is Number -> TimestampFrameId(jsonValue.toLong())
            is String -> SupplementalFrameId(jsonValue)
            else -> throw JSONException("Unknown FrameId type")
        }
    }

    private fun frameIdToJson(frameId: FrameId) =
        when (frameId) {
            is TimestampFrameId -> frameId.milliseconds
            is SupplementalFrameId -> frameId.label
        }

    private fun featureFromJson(
        jsonObject: JSONObject,
        typeRegistry: Map<String, DataPointType<*>>,
    ): Feature<*> {
        val name = jsonObject.getString(KEY_FEATURE_NAME)
        val type = typeRegistry[jsonObject.optString(KEY_FEATURE_TYPE)] ?: unknownType

        val dataPoints =
            jsonObject.getJSONArray(KEY_FEATURE_DATAPOINTS).convert(JSONArray::get, type::fromJson)
        return Feature(name, dataPoints)
    }

    private fun featureToJson(feature: Feature<*>) =
        JSONObject().apply {
            put(KEY_FEATURE_NAME, feature.name)

            val dataPointTypes =
                feature.dataPoints
                    .filterIsInstance<ValueDataPoint<Any>>()
                    .map { it.type.typeName }
                    .toSet()
            if (dataPointTypes.size == 1) {
                put(KEY_FEATURE_TYPE, dataPointTypes.single())
            } else if (dataPointTypes.size > 1) {
                throw JSONException(
                    "Feature [${feature.name}] contains more than one data point type: " +
                        "[${dataPointTypes.joinToString()}]"
                )
            }

            put(
                KEY_FEATURE_DATAPOINTS,
                JSONArray().apply { feature.dataPoints.map { it.asJson() }.forEach(this::put) },
            )
        }

    private const val KEY_FRAME_IDS = "frame_ids"
    private const val KEY_FEATURES = "features"
    private const val KEY_FEATURE_NAME = "name"
    private const val KEY_FEATURE_TYPE = "type"
    private const val KEY_FEATURE_DATAPOINTS = "data_points"

    private val unknownType: DataPointType<Any> =
        DataPointType(
            "unknown",
            jsonToValue = { throw UnknownTypeException() },
            valueToJson = { throw AssertionError() },
        )
}

/** Creates a type registry from the types used in the [TimeSeries]. */
fun TimeSeries.createTypeRegistry(): Map<String, DataPointType<*>> = buildMap {
    for (feature in features.values) {
        for (dataPoint in feature.dataPoints) {
            if (dataPoint is ValueDataPoint) {
                val type = dataPoint.type
                val alreadyRegisteredType = put(type.typeName, type)
                if (alreadyRegisteredType != null && alreadyRegisteredType != type) {
                    throw AssertionError(
                        "Type [${type.typeName}] with multiple different implementations"
                    )
                }
            }
        }
    }
}

private fun <I, O> JSONArray.convert(
    elementAccessor: JSONArray.(index: Int) -> I,
    convertFn: (I) -> O,
) = buildList {
    for (i in 0 until length()) {
        add(convertFn(elementAccessor(i)))
    }
}

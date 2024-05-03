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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.UnknownTypeException

fun Dp.asDataPoint() = DataPointTypes.dp.makeDataPoint(this)

fun IntSize.asDataPoint() = DataPointTypes.intSize.makeDataPoint(this)

fun DpSize.asDataPoint() = DataPointTypes.dpSize.makeDataPoint(this)

fun DpOffset.asDataPoint() = DataPointTypes.dpOffset.makeDataPoint(this)

object DataPointTypes {

    val dp: DataPointType<Dp> =
        DataPointType(
            "dp",
            jsonToValue = {
                when (it) {
                    is Float -> it.dp
                    is Number -> it.toFloat().dp
                    is String -> it.toFloatOrNull()?.dp ?: throw UnknownTypeException()
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = { it.value }
        )

    val intSize: DataPointType<IntSize> =
        DataPointType(
            "intSize",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    IntSize(getInt("width"), getInt("height"))
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("width", it.width)
                    put("height", it.height)
                }
            }
        )

    val dpSize: DataPointType<DpSize> =
        DataPointType(
            "dpSize",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    DpSize(getDouble("width").dp, getDouble("height").dp)
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("width", it.width.value)
                    put("height", it.height.value)
                }
            }
        )

    val dpOffset: DataPointType<DpOffset> =
        DataPointType(
            "dpOffset",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    DpOffset(getDouble("x").dp, getDouble("y").dp)
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("x", it.x.value)
                    put("y", it.y.value)
                }
            }
        )
}

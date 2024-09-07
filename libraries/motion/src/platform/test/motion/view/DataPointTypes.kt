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

package platform.test.motion.view

import android.graphics.Point
import android.graphics.Rect
import org.json.JSONObject
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.UnknownTypeException

fun Rect.asDataPoint() = DataPointTypes.rect.makeDataPoint(this)

fun Point.asDataPoint() = DataPointTypes.point.makeDataPoint(this)

/** [DataPointType] implementations for core [View] related types. */
object DataPointTypes {
    val point: DataPointType<Point> =
        DataPointType(
            "point",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    Point(getInt("x"), getInt("y"))
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("x", it.x)
                    put("y", it.y)
                }
            },
            ensureImmutable = { Point(it) }
        )

    val rect: DataPointType<Rect> =
        DataPointType(
            "rect",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    Rect(getInt("left"), getInt("top"), getInt("right"), getInt("bottom"))
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("left", it.left)
                    put("top", it.top)
                    put("right", it.right)
                    put("bottom", it.bottom)
                }
            },
            ensureImmutable = { Rect(it) }
        )

    /** [GradientDrawable] corner radii */
    val cornerRadii: DataPointType<CornerRadii> =
        DataPointType(
            "cornerRadii",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    CornerRadii(
                        FloatArray(length()) { index ->
                            getDouble(cornerRadiiPropertyNames[index]).toFloat()
                        }
                    )
                }
            },
            valueToJson = {
                JSONObject().apply {
                    for (i in it.rawValues.indices) {
                        put(cornerRadiiPropertyNames[i], it.rawValues[i])
                    }
                }
            }
        )
    // property names match order of val
    private val cornerRadiiPropertyNames =
        listOf(
            "top_left_x",
            "top_left_y",
            "top_right_x",
            "top_right_y",
            "bottom_right_x",
            "bottom_right_y",
            "bottom_left_x",
            "bottom_left_y",
        )

    val allTypes = listOf(point, rect, cornerRadii)
}

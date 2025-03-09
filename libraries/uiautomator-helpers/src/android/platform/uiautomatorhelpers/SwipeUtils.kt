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
package android.platform.uiautomatorhelpers

import android.graphics.Point
import android.graphics.Rect
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Direction.DOWN
import androidx.test.uiautomator.Direction.LEFT
import androidx.test.uiautomator.Direction.RIGHT
import androidx.test.uiautomator.Direction.UP

/** Common utils to perform swipes. */
internal object SwipeUtils {

    /**
     * Calculates start and end point taking into consideration first [marginPx], then [percent].
     */
    fun calculateStartEndPoint(
        rawBound: Rect,
        direction: Direction,
        percent: Float = 1.0f,
        marginPx: Int = 0
    ): Pair<Point, Point> {
        val bounds = Rect(rawBound)
        bounds.inset(marginPx, marginPx)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        return when (direction) {
            LEFT -> {
                Point(bounds.right, centerY) to
                    Point(bounds.right - (bounds.width() * percent).toInt(), centerY)
            }
            RIGHT -> {
                Point(bounds.left, centerY) to
                    Point(bounds.left + (bounds.width() * percent).toInt(), centerY)
            }
            UP -> {
                Point(centerX, bounds.bottom) to
                    Point(centerX, bounds.bottom - (bounds.height() * percent).toInt())
            }
            DOWN -> {
                Point(centerX, bounds.top) to
                    Point(centerX, bounds.top + (bounds.height() * percent).toInt())
            }
        }
    }
}

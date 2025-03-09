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

import android.graphics.Rect
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.SwipeUtils.calculateStartEndPoint
import android.platform.uiautomatorhelpers.TracingUtils.trace
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.time.Duration

/**
 * A scroll utility that should be used instead of [UiObject2.scroll] for more reliable scrolls.
 *
 * See [BetterSwipe] for more details on the problem of [UiObject2.scroll].
 */
object BetterScroll {
    private const val DEFAULT_PERCENTAGE = 0.8f
    private val DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(1)

    /**
     * Scrolls [percentage] of [rect] in the given [direction].
     *
     * Note that when direction is [Direction.DOWN], the scroll will be from the top to the bottom
     * (to scroll down).
     */
    @JvmStatic
    @JvmOverloads
    fun scroll(
        rect: Rect,
        direction: Direction,
        percentage: Float = DEFAULT_PERCENTAGE,
    ) {
        val (start, stop) = calculateStartEndPoint(rect, direction, percentage)

        trace("Scrolling $start -> $stop") {
            uiDevice.performActionAndWait(
                { BetterSwipe.from(start).to(stop).pause().release() },
                Until.scrollFinished(Direction.reverse(direction)),
                DEFAULT_WAIT_TIMEOUT.toMillis()
            )
        }
    }
}

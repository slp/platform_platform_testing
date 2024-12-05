/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.platform.systemui_tapl.ui

import android.graphics.Point
import android.platform.systemui_tapl.utils.DeviceUtils.launcherResSelector
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Provides an API for interacting with the collapsed bubble bar within launcher in UI automation
 * tests.
 *
 * Note that this class does not represent the state of the bubble bar being stashed.
 *
 * @see [ExpandedBubbleBar]
 */
class BubbleBar {

    init {
        BUBBLE_BAR_VIEW.assertVisible { "Failed while waiting for bubble bar to become visible" }
        assertThat(bubbles).isNotEmpty()
    }

    /**
     * Returns the selected bubble in the bubble bar.
     *
     * Bubbles in the collapsed bubble bar are reversed. The selected bubble is the last bubble in
     * the view hierarchy.
     */
    val selectedBubble: BubbleBarItem
        get() = bubbles.last()

    /**
     * Returns all the bubbles in the bubble bar.
     *
     * Note that the overflow bubble is not included in the result because it is never visible when
     * the bubble bar is collapsed.
     */
    val bubbles: List<BubbleBarItem>
        get() = waitForObj(BUBBLE_BAR_VIEW).children.map { BubbleBarItem(it) }

    /** Expands the bubble bar by clicking on it and returns [ExpandedBubbleBar]. */
    fun expand(): ExpandedBubbleBar {
        BUBBLE_BAR_VIEW.click()
        return ExpandedBubbleBar(selectedBubble)
    }

    /** Expands the bubble bar by swiping up on it and returns [ExpandedBubbleBar]. */
    fun swipeUpToExpand(): ExpandedBubbleBar {
        val bubbleBarCenter = waitForObj(BUBBLE_BAR_VIEW).visibleCenter
        val windowHeight = uiDevice.displayHeight
        // we want to swipe to the point that is twice as far from the bottom of the screen as the
        // bubble bar center Y coordinate
        val destinationY = 2 * bubbleBarCenter.y - windowHeight
        BetterSwipe.from(Point(bubbleBarCenter.x, windowHeight))
            .to(Point(bubbleBarCenter.x, destinationY))
            .release()
        return ExpandedBubbleBar(selectedBubble)
    }

    /**
     * Drags the bubble bar to the dismiss target. At the end of the gesture the bubble bar will be
     * gone.
     */
    fun dragToDismiss() {
        BetterSwipe.from(waitForObj(BUBBLE_BAR_VIEW).visibleCenter)
            .pause()
            .to(
                waitForObj(DISMISS_VIEW).visibleCenter,
                Duration.of(500, ChronoUnit.MILLIS),
                PRECISE_GESTURE_INTERPOLATOR,
            )
            .release()

        BUBBLE_BAR_VIEW.assertInvisible {
            "Failed while waiting for bubble bar to become invisible"
        }
    }

    companion object {
        internal val BUBBLE_BAR_VIEW = launcherResSelector("taskbar_bubbles")
        private val DISMISS_VIEW = launcherResSelector("dismiss_view")
    }
}

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

package android.platform.systemui_tapl.ui

import android.graphics.PointF
import android.platform.helpers.CommonUtils
import android.platform.systemui_tapl.ui.Bubble.Companion.bubbleViews
import android.platform.systemui_tapl.utils.DeviceUtils.launcherResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.context
import android.platform.uiautomatorhelpers.DeviceHelpers.hasObject
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForNullableObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.FLING_GESTURE_INTERPOLATOR
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.test.uiautomator.UiObject2
import com.android.launcher3.tapl.LauncherInstrumentation
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import java.time.temporal.ChronoUnit.MILLIS

/** System UI test automation object representing the expanded bubble stack. */
class ExpandedBubbleStack internal constructor() {
    init {
        BUBBLE_EXPANDED_VIEW.assertVisible(timeout = FIND_OBJECT_TIMEOUT) {
            "Bubbles expanded view should be visible"
        }
    }

    /** Returns all bubbles. */
    val bubbles: List<Bubble>
        get() = bubbleViews.map { bubbleView: UiObject2 -> Bubble(bubbleView) }

    /** Clicks the overflow button and returns the overflow panel that appears. */
    fun openOverflow(): BubbleOverflow {
        bubbleOverflow.click()
        return BubbleOverflow()
    }

    /** Closes the stack by swiping up. */
    fun closeBySwiping() {
        val windowBounds = windowMetrics.bounds
        val x = windowBounds.width() / 2f
        // Move up by 1 pixel to ensure tap begins on screen.
        val startY = windowBounds.bottom - 1f
        // From bottom middle of screen
        val from = PointF(x, startY)
        // To middle of screen
        val to = PointF(x, startY / 2)
        // Use a custom duration for bubble swipe to reduce flakiness on slow device.
        BetterSwipe.from(from)
            .to(to, duration = Duration.of(700, MILLIS), interpolator = FLING_GESTURE_INTERPOLATOR)
            .release()
        Root.get().verifyNoExpandedBubbleStackIsVisible()
    }

    /** Closes the stack by the "back" gesture. */
    fun closeByBackGesture() {
        LauncherInstrumentation().pressBack()
        Root.get().verifyNoExpandedBubbleStackIsVisible()
    }

    /** Closes the stack by clicking outside. */
    fun closeByClickingOutside() {
        val gestureInsets =
            windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.mandatorySystemGestures() or WindowInsets.Type.displayCutout()
            )
        val clickX = gestureInsets.left
        val clickY = gestureInsets.top
        uiDevice.click(clickX, clickY)
        Root.get().verifyNoExpandedBubbleStackIsVisible()
    }

    /** Dismiss Manage education to proceed with expanded bubbles */
    fun dismissManageEducation() {
        if (hasObject(BUBBLE_MANAGE_EDUCATION)) {
            waitForObj(BUBBLE_GOT_IT_BUTTON).click()
            uiDevice.waitForIdle()
        }
    }

    companion object {
        val FIND_OBJECT_TIMEOUT = Duration.ofSeconds(20)
        val BUBBLE_EXPANDED_VIEW = sysuiResSelector("bubble_expanded_view")
        private val BUBBLE_OVERFLOW_BUTTON = sysuiResSelector("bubble_overflow_button")
        private val BUBBLE_BAR_OVERFLOW = launcherResSelector("bubble_overflow_button")
        private val BUBBLE_MANAGE_EDUCATION = sysuiResSelector("manage_education_view")
        private val BUBBLE_GOT_IT_BUTTON = sysuiResSelector("got_it")

        private val windowMetrics: WindowMetrics
            get() = context.getSystemService(WindowManager::class.java)!!.currentWindowMetrics

        @JvmStatic
        internal val bubbleOverflow: UiObject2
            get() {
                val bubbleOverflow =
                    if (CommonUtils.isLargeScreen()) {
                        // Check bubble bar first if we're large screen
                        waitForNullableObj(BUBBLE_BAR_OVERFLOW, timeout = FIND_OBJECT_TIMEOUT)
                            ?:
                            // Check floating in case bubble bar wasn't active
                            waitForNullableObj(
                                BUBBLE_OVERFLOW_BUTTON,
                                timeout = FIND_OBJECT_TIMEOUT,
                            )
                    } else {
                        waitForNullableObj(BUBBLE_OVERFLOW_BUTTON, timeout = FIND_OBJECT_TIMEOUT)
                    }
                assertWithMessage("Bubble overflow not visible").that(bubbleOverflow).isNotNull()
                return bubbleOverflow!!
            }
    }
}

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
import android.os.SystemClock
import android.platform.helpers.CommonUtils
import android.platform.systemui_tapl.utils.DeviceUtils.launcherResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.context
import android.platform.uiautomatorhelpers.DeviceHelpers.hasObject
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForPossibleEmpty
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import android.view.WindowInsets
import android.view.WindowManager
import androidx.test.uiautomator.UiObject2
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * System UI test automation object representing a notification bubble, specifically the view
 * representing the bubble, shown when the stack is collapsed or expanded.
 */
class Bubble internal constructor(private val bubbleView: UiObject2) {
    /** Expands the bubble into the stack. */
    fun expand(): ExpandedBubbleStack {
        bubbleView.click()
        // check if bubble stack education is visible
        // education visibility can be checked only after interacting with the bubble (click)
        // it might be invoked by user interaction, if it wasn't presented yet
        if (isEducationVisible) {
            // click bubble again to expand
            // if education is visible, the previous interaction didn't expand the bubble stack
            bubbleView.click()
        }
        // let the stack expand animation to finish
        SystemClock.sleep(STACK_EXPAND_TIMEOUT.toMillis())
        uiDevice.waitForIdle()
        return ExpandedBubbleStack().apply {
            // dismiss manage education if it's visible
            dismissManageEducation()
        }
    }

    /** Clicks on the bubble. */
    fun click() {
        bubbleView.click()
    }

    /** Returns the content description of the bubble. */
    fun contentDescription(): String = bubbleView.contentDescription

    /** Dismisses the bubble by dragging it to the Dismiss target. */
    fun dismiss() {
        dragBubbleToDismiss()
        // check if bubble stack education is visible and blocked interaction
        // education visibility can be checked only after interacting with the bubble (swipe)
        // it might be invoked by user interaction, if it wasn't presented yet
        if (isEducationVisible) {
            // retry drag interaction
            // if education is visible, the previous interaction was blocked and didn't drag bubble
            dragBubbleToDismiss()
        }
    }

    /** Returns the flyout if it's visible, or fails. */
    val flyout: BubbleFlyout
        get() = BubbleFlyout()

    /** Is Welcome education visible and stopped bubbles expand */
    private val isEducationVisible: Boolean
        get() = hasObject(BUBBLE_STACK_EDUCATION)

    /** Drag bubble to Dismiss target */
    private fun dragBubbleToDismiss() {
        val windowMetrics =
            context.getSystemService(WindowManager::class.java)!!.currentWindowMetrics
        val insets =
            windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.mandatorySystemGestures() or
                    WindowInsets.Type.navigationBars() or
                    WindowInsets.Type.displayCutout()
            )
        val destination =
            PointF(
                windowMetrics.bounds.width() / 2f,
                (windowMetrics.bounds.height() - insets.bottom).toFloat(),
            )
        BetterSwipe.from(bubbleView.visibleCenter)
            .to(
                destination,
                duration = Duration.of(700, ChronoUnit.MILLIS),
                interpolator = PRECISE_GESTURE_INTERPOLATOR,
            )
            .release()
    }

    companion object {
        val FIND_OBJECT_TIMEOUT = Duration.ofSeconds(20)
        val BUBBLE_VIEW = sysuiResSelector("bubble_view")
        val BUBBLE_BAR_VIEWS = launcherResSelector("bubble_view")
        private val STACK_EXPAND_TIMEOUT = Duration.ofSeconds(1)
        private val BUBBLE_STACK_EDUCATION = sysuiResSelector("stack_education_layout")

        @JvmStatic
        internal val bubbleViews: List<UiObject2>
            get() {
                val bubbleViews =
                    if (CommonUtils.isLargeScreen()) {
                        // Check bubble bar first if we're large screen
                        waitForPossibleEmpty(BUBBLE_BAR_VIEWS, timeout = FIND_OBJECT_TIMEOUT)
                            .ifEmpty {
                                // Check floating in case bubble bar wasn't active
                                waitForPossibleEmpty(BUBBLE_VIEW, timeout = FIND_OBJECT_TIMEOUT)
                            }
                    } else {
                        waitForPossibleEmpty(BUBBLE_VIEW, timeout = FIND_OBJECT_TIMEOUT)
                    }
                assertWithMessage("No bubbles visible").that(bubbleViews.size).isAtLeast(1)
                return bubbleViews
            }
    }
}

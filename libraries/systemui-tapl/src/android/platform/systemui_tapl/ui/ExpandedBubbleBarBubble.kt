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
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import android.view.WindowManager

/**
 * Provides an API for interacting with the expanded bubble view when bubble bar is used in UI
 * automation tests.
 *
 * @see [ExpandedBubbleBar]
 */
class ExpandedBubbleBarBubble internal constructor() {

    init {
        BUBBLE_EXPANDED_VIEW.assertVisible()
        HANDLE_VIEW.assertVisible()
    }

    /** Dismisses expanded view by dragging it into the dismiss area */
    fun dragToDismiss() {
        val windowMetrics =
            DeviceHelpers.context.getSystemService(WindowManager::class.java)!!.currentWindowMetrics
        val displayCenter = Point(windowMetrics.bounds.centerX(), windowMetrics.bounds.centerY())

        BetterSwipe.from(waitForObj(HANDLE_VIEW).visibleCenter)
            // First drag to the center of the display, dismiss view only shows up after drag starts
            .to(displayCenter, interpolator = PRECISE_GESTURE_INTERPOLATOR)
            .to(waitForObj(DISMISS_VIEW).visibleCenter, interpolator = PRECISE_GESTURE_INTERPOLATOR)
            .release()

        BUBBLE_EXPANDED_VIEW.assertInvisible {
            "Failed while waiting for expanded bubble to become invisible"
        }
    }

    private companion object {
        val BUBBLE_EXPANDED_VIEW = sysuiResSelector("bubble_expanded_view")
        val HANDLE_VIEW = sysuiResSelector("bubble_bar_handle_view")
        val DISMISS_VIEW = sysuiResSelector("dismiss_view")
    }
}

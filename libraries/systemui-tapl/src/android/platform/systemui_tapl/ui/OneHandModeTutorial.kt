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

import android.graphics.Rect
import android.platform.systemui_tapl.ui.NotificationShade.Companion.waitForShadeToClose
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.betterSwipe
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics

/**
 * System UI test automation object representing swipe down screen bottom to trigger one-handed mode
 * triggered, the whole screen translate down and tutorial shown on top area.
 * HSV:https://hsv.googleplex.com/4846680840077312 Lastly, swipe down gesture to close one-handed
 * mode, and ensure the display area recovered. Swipe gesture
 * region:https://screenshot.googleplex.com/q83UXMidE2PUFDq)
 */
class OneHandModeTutorial internal constructor() {

    init {
        waitForShadeToClose()
        TUTORIAL_SELECTOR.assertVisible()
    }

    /**
     * Closes the one-handed mode by swipe up gesture at screen bottom area. Gesture region:
     * https://screenshot.googleplex.com/q83UXMidE2PUFDq, and make sure the tutorial invisible from
     * screen at the end.
     */
    fun close() {
        val windowMetrics: WindowMetrics =
            DeviceHelpers.context.getSystemService(WindowManager::class.java)!!.currentWindowMetrics
        val insets: WindowInsets = windowMetrics.windowInsets
        val displayBounds: Rect = windowMetrics.bounds
        val bottomMandatoryGestureHeight: Int =
            insets
                .getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
                )
                .bottom

        // Swipe up from screen bottom exiting one-handed mode in System Gesture mandatory region.
        betterSwipe(
            displayBounds.width() / 2,
            displayBounds.height(),
            displayBounds.width() / 2,
            displayBounds.height() - Math.round(bottomMandatoryGestureHeight * 2.5f),
        )

        TUTORIAL_SELECTOR.assertInvisible()
    }

    companion object {
        private val TUTORIAL_SELECTOR = sysuiResSelector("one_handed_tutorial_layout")
        private const val TRANSITION_TIMEOUT = 20000
    }
}

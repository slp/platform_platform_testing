/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.doubleTapAt
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForFirstObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.assertOnTheLeftSide
import android.platform.uiautomatorhelpers.assertOnTheRightSide
import java.time.Duration

/**
 * User switcher visible on lockscreen on large screen devices when
 * [USER_SWITCHER_VISIBLE_FLAG_NAME] is enabled. Constructor fails if the switcher is not visible.
 */
class LockscreenUserSwitcher internal constructor() {

    private val uiObject =
        waitForFirstObj(*USER_SWITCHER_SELECTORS, timeout = LONG_WAIT) {
                "Lockscreen user switcher not found"
            }
            .first

    /** Check that the bouncer is on the right side of the screen. */
    fun assertOnTheRightSide(): Unit = uiObject.assertOnTheRightSide()

    /** Check that the bouncer is on the left side of the screen. */
    fun assertOnTheLeftSide(): Unit = uiObject.assertOnTheLeftSide()

    /**
     * Double tap the bottom of the view. Used for large screen where user switcher is side by side
     * with bouncer and double tapping means they should switch sides horizontally
     */
    fun doubleTapBelowUserSwitcher() {
        val bounds = uiObject.visibleBounds
        // Tap 5 pixels below the user switcher UI element.
        val (touchX, touchY) = Pair(bounds.centerX(), bounds.bottom + 5)
        uiDevice.doubleTapAt(touchX, touchY)
    }

    /** Taps the user switcher anchor to expand the list. */
    fun openUserSelectorExpandedList(): UserSelectionExpandedList {
        waitForObj(USER_SWITCHER_DROPDOWN_SELECTOR) { "User switcher anchor not found" }.click()
        return UserSelectionExpandedList()
    }

    private companion object {
        // Default wait used by waitForObj. waitForFirstObj uses a shorter wait.
        private val LONG_WAIT = Duration.ofSeconds(10)

        // https://hsv.googleplex.com/6328527512141824?node=23
        private val USER_SWITCHER_DROPDOWN_SELECTOR = sysuiResSelector("user_switcher_anchor")
        private val USER_SWITCHER_SELECTORS =
            arrayOf(
                sysuiResSelector("keyguard_bouncer_user_switcher"),
                sysuiResSelector("UserSwitcher"),
            )
    }
}

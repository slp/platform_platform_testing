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

import android.platform.helpers.LockscreenUtils.LockscreenType
import android.platform.helpers.LockscreenUtils.LockscreenType.PASSWORD
import android.platform.helpers.LockscreenUtils.LockscreenType.PIN
import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.settingsResSelector
import android.platform.test.scenario.tapl_common.Gestures
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.shell
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import java.time.Duration

/**
 * System UI test automation object representing the setup screen of screen lock.
 *
 * https://hsv.googleplex.com/6685423272198144
 */
class ChooseScreenLock internal constructor() {

    init {
        CHOOSE_A_SCREEN_LOCK_SELECTOR.assertVisible(timeout = LONG_WAIT_TIME)
    }

    /**
     * Choose one screen lock.
     *
     * @param screenLockType type of screen lock set.
     */
    fun chooseScreenLock(screenLockType: LockscreenType) {
        waitForObj(androidResSelector("title").text(screenLockType.toString())).click()
    }

    /**
     * Entering the given code on the setup page of screen lock.
     *
     * If using the setLockscreenPin method to set a 6 digit PIN lock, it will not trigger the auto
     * unlock feature, so add the functionality to set PIN lock.
     *
     * @param screenLockType type of screen lock set.
     * @param screenLockCode screen lock code.
     */
    fun enterCodeOnSetupPageOfScreenLock(screenLockType: LockscreenType, screenLockCode: String) {
        when (screenLockType) {
            PIN,
            PASSWORD -> {
                waitForObj(PASSWORD_ENTRY, LONG_WAIT_TIME).click()
                uiDevice.shell("input keyboard text $screenLockCode")
                Gestures.click(waitForObj(NEXT_BUTTON_SELECTOR, LONG_WAIT_TIME), "Next button")
                uiDevice.shell("input keyboard text $screenLockCode")
                Gestures.click(
                    waitForObj(CONFIRM_BUTTON_SELECTOR, LONG_WAIT_TIME),
                    "Confirm button",
                )
                Gestures.click(waitForObj(DONE_BUTTON_SELECTOR, LONG_WAIT_TIME), "Done button")
                DONE_BUTTON_SELECTOR.assertInvisible(timeout = LONG_WAIT_TIME)
            }
            else -> throw AssertionError("Non-supported Lockscreen Type: $screenLockType")
        }
    }

    private companion object {
        @JvmField val LONG_WAIT_TIME: Duration = Duration.ofSeconds(15)

        const val BUTTON_CLASS = "android.widget.Button"

        // https://hsv.googleplex.com/6685423272198144?node=7
        val CHOOSE_A_SCREEN_LOCK_SELECTOR: BySelector =
            settingsResSelector("collapsing_toolbar").desc("Choose a screen lock")

        val PASSWORD_ENTRY: BySelector = settingsResSelector("password_entry")

        val NEXT_BUTTON_SELECTOR: BySelector = By.clazz(BUTTON_CLASS).text("Next")

        val CONFIRM_BUTTON_SELECTOR: BySelector = By.clazz(BUTTON_CLASS).text("Confirm")

        // https://hsv.googleplex.com/5618542457126912?node=26
        val DONE_BUTTON_SELECTOR: BySelector = By.clazz(BUTTON_CLASS).text("Done")
    }
}

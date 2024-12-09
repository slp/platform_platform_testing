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

import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.UserUtils.runThenWaitUntilSwitchCompleted
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import java.util.regex.Pattern

/**
 * System UI test automation object representing the exit confirmation dialog when either switching
 * from an ephemeral guest user, or pressing "Exit guest" when device is configured with
 * config_guestUserAutoCreated.
 *
 * https://hsv.googleplex.com/5025959268843520
 */
class ExitGuestPrompt internal constructor() {
    /** Click the exit button. https://hsv.googleplex.com/5025959268843520?node=16 */
    fun confirmExit() {
        runThenWaitUntilSwitchCompleted {
            TITLE_SELECTOR.assertVisible { "Exit guest prompt prompt dialog is not visible" }
            waitForObj(EXIT_SELECTOR) { "Exit button not found" }.click()
        }
    }

    companion object {

        // https://hsv.googleplex.com/6067368607350784?node=10
        private val TITLE_SELECTOR =
            androidResSelector("alertTitle")
                .text(Pattern.compile("Exit guest mode\\?", Pattern.CASE_INSENSITIVE))

        // https://hsv.googleplex.com/6067368607350784?node=19
        private val EXIT_SELECTOR =
            androidResSelector("button1").text(Pattern.compile("Exit", Pattern.CASE_INSENSITIVE))
    }
}

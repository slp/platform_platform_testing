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
 * System UI test automation object representing the confirmation dialog after switching to the
 * guest user.
 */
class WelcomeBackGuestDialog internal constructor() {
    /** Click the start over button. https://hsv.googleplex.com/6260902091292672?node=15 */
    fun startOver() {
        runThenWaitUntilSwitchCompleted {
            TITLE_SELECTOR.assertVisible { "Welcome back guest dialog is not visible" }
            waitForObj(START_OVER_BUTTON_SELECTOR).click()
        }
    }

    companion object {
        // https://hsv.googleplex.com/6260902091292672?node=7
        private val TITLE_SELECTOR =
            androidResSelector("alertTitle")
                .text(Pattern.compile("Welcome back, guest!", Pattern.CASE_INSENSITIVE))

        // https://hsv.googleplex.com/6260902091292672?node=15
        private val START_OVER_BUTTON_SELECTOR =
            androidResSelector("button2")
                .text(Pattern.compile("Start over", Pattern.CASE_INSENSITIVE))
    }
}

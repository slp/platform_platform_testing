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

import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.UserUtils.runThenWaitUntilSwitchCompleted
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import java.util.regex.Pattern

/**
 * System UI test automation object representing the panel for adding the new user.
 * https://hsv.googleplex.com/6238141703782400
 */
class AddUserPanel internal constructor() {

    init {
        TITLE_SELECTOR.assertVisible { "Add user panel is not visible" }
    }

    /** Clicks on the OK button. */
    fun OK() {
        runThenWaitUntilSwitchCompleted {
            waitForObj(OK_SELECTOR) { "OK Button not found" }.click()
        }
    }

    /** Clicks on the cancel button. */
    fun cancel() {
        waitForObj(CANCEL_SELECTOR) { "Cancel button not found" }.click()
    }

    /** Sets the user name. */
    fun setUserName(userName: String) {
        val editNameField = waitForObj(EDIT_NAME_SELECTOR) { "Name edit field not found" }
        editNameField.click()
        editNameField.text = userName
    }

    companion object {
        // https://hsv.googleplex.com/6238141703782400?node=7
        private val TITLE_SELECTOR =
            androidResSelector("alertTitle")
                .text(Pattern.compile("Add user", Pattern.CASE_INSENSITIVE))

        // https://hsv.googleplex.com/6238141703782400?node=18
        private val OK_SELECTOR =
            androidResSelector("button1").text(Pattern.compile("OK", Pattern.CASE_INSENSITIVE))

        // https://hsv.googleplex.com/6238141703782400?node=17
        private val CANCEL_SELECTOR =
            androidResSelector("button2").text(Pattern.compile("Cancel", Pattern.CASE_INSENSITIVE))

        // https://hsv.googleplex.com/6238141703782400?node=17
        private val EDIT_NAME_SELECTOR = sysuiResSelector("user_name")
    }
}

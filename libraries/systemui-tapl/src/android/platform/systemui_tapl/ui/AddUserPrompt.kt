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

import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import java.util.regex.Pattern

/**
 * System UI test automation object representing the dialog for adding the new user from quick
 * settings.
 *
 * https://hsv.googleplex.com/5360066535358464
 */
class AddUserPrompt internal constructor() {
    /** Clicks on the OK button. https://hsv.googleplex.com/5360066535358464?node=15 */
    fun clickOkToOpenAddUserPanel(): AddUserPanel {
        TITLE_SELECTOR.assertVisible()
        waitForObj(OK_SELECTOR) { "OK button not found" }.click()
        return AddUserPanel()
    }

    /** Clicks on the cancel button. https://hsv.googleplex.com/5360066535358464?node=14 */
    fun cancel() {
        TITLE_SELECTOR.assertVisible()
        waitForObj(CANCEL_SELECTOR) { "Cancel button not found" }.click()
    }

    companion object {
        // https://hsv.googleplex.com/5360066535358464?node=11
        private val TITLE_SELECTOR =
            By.res("com.android.systemui:id/dialog_with_icon_title")
                .text(Pattern.compile("Add new user\\?", Pattern.CASE_INSENSITIVE))

        // https://hsv.googleplex.com/5360066535358464?node=15
        private val OK_SELECTOR =
            By.res("com.android.systemui:id/button_ok")
                .text(Pattern.compile("Next", Pattern.CASE_INSENSITIVE))

        // https://hsv.googleplex.com/5360066535358464?node=14
        private val CANCEL_SELECTOR =
            By.res("com.android.systemui:id/button_cancel")
                .text(Pattern.compile("Cancel", Pattern.CASE_INSENSITIVE))
    }
}

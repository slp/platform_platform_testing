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
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import com.android.launcher3.tapl.LauncherInstrumentation

/** System UI test automation object representing an alert dialog. */
class AlertDialog internal constructor() {

    init {
        UI_DIALOG_TITLE.assertVisible()
    }

    /** Asserts visibility of the 'negative' button in the dialog */
    fun assertNegativeButtonVisibility(visible: Boolean) {
        uiDevice.assertVisibility(androidResSelector(UI_ALERT_DIALOG_NEGATIVE_BUTTON_ID), visible)
    }

    /* Dismisses the dialog by the Back gesture */
    fun dismiss() {
        // Press back to dismiss the dialog
        LauncherInstrumentation().pressBack()
        UI_DIALOG_TITLE.assertInvisible()
    }

    companion object {
        private const val UI_DIALOG_TITLE_ID = "alertTitle"
        private const val UI_ALERT_DIALOG_NEGATIVE_BUTTON_ID = "button2"
        private val UI_DIALOG_TITLE = androidResSelector(UI_DIALOG_TITLE_ID)
    }
}

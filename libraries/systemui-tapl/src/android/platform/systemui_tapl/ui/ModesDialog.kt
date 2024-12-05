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

import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import com.android.launcher3.tapl.LauncherInstrumentation
import java.time.Duration

/** System UI test automation object representing the modes list dialog. */
class ModesDialog internal constructor() {

    init {
        UI_DIALOG_TITLE.assertVisible()
    }

    /* Dismisses the dialog by the Back gesture */
    fun dismiss() {
        // Press back to dismiss the dialog
        LauncherInstrumentation().pressBack()
        UI_DIALOG_TITLE.assertInvisible()
    }

    private fun getModeTile(modeName: String): UiObject2 {
        return waitForObj(By.text(modeName)).parent
    }

    fun tapMode(modeName: String) {
        getModeTile(modeName).click()
    }

    fun verifyModeState(modeName: String, isOn: Boolean) {
        val modeTile = getModeTile(modeName)
        modeTile.waitForObj(
            if (isOn) MODE_STATE_ON_SELECTOR else MODE_STATE_OFF_SELECTOR,
            errorProvider = { "Tile not in correct state, wanted ${if (isOn) "On" else "Off"}" },
        )
    }

    companion object {
        private const val UI_DIALOG_TITLE_ID = "modes_title"
        private const val UI_ALERT_DIALOG_NEGATIVE_BUTTON_ID = "button2"
        private val UI_DIALOG_TITLE = By.res(UI_DIALOG_TITLE_ID)
        private val MODE_NAME_SELECTOR = By.res("name")
        private val MODE_STATE_ON_SELECTOR = By.res("stateOn")
        private val MODE_STATE_OFF_SELECTOR = By.res("stateOff")
        private val UI_RESPONSE_TIMEOUT = Duration.ofSeconds(3)
    }
}

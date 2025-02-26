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

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.WaitResult
import android.platform.uiautomatorhelpers.WaitUtils.waitToBecomeTrue
import android.platform.uiautomatorhelpers.scrollUntilFound
import android.util.Log

/** Wrapper representing the BluetoothDialog that opens when the QS Tile is clicked */
class BluetoothDialog internal constructor() {
    val scrollView =
        waitForObj(
            sysuiResSelector(SCROLL_VIEW_RES_ID)
                .hasParent(sysuiResSelector(BLUETOOTH_TILE_DIALOG_RES_ID))
        )

    /** Finds the done button, clicks on it and asserts that the dialog has closed. */
    fun clickOnDoneAndClose() {
        val doneButton = scrollView.scrollUntilFound(DONE_BTN) ?: error("Done button not found")
        doneButton.click()
        if (waitToBecomeTrue { !uiDevice.hasObject(DONE_BTN) }.result !is WaitResult.WaitSuccess) {
            Log.d("QuickSettingsTileBase", "Retrying click due to b/339676505")
            doneButton.click()
        }
        DONE_BTN.assertInvisible(errorProvider = { "Bluetooth tile dialog is dismissed" })
    }

    private companion object {
        val DONE_BTN = sysuiResSelector("done_button")
        const val SCROLL_VIEW_RES_ID = "scroll_view"
        const val BLUETOOTH_TILE_DIALOG_RES_ID = "root"
    }
}

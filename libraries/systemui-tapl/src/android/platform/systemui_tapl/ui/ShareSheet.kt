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

import android.graphics.Point
import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat

/** System UI test automation object representing the share sheet. */
class ShareSheet internal constructor() {
    init {
        TaplUiDevice.waitForObject(
            androidResSelector(RESOLVER_LIST_ID),
            objectName = "share sheet page",
        )
    }

    private fun hasObjectWithWait(selector: BySelector): Boolean {
        return uiDevice.wait(Until.hasObject(selector), UI_RESPONSE_TIMEOUT_MSECS.toLong())
    }

    /** Pull the share sheet page to top of the screen. */
    fun pullUp() {
        val device = uiDevice
        assertThat(hasObjectWithWait(HEADER_SELECTOR)).isTrue()
        val header = uiDevice.waitForObj(HEADER_SELECTOR)
        header.drag(Point(device.displayWidth / 2, 0), device.displayHeight / 5)
        device.waitForIdle(UI_RESPONSE_TIMEOUT_MSECS.toLong())
    }

    private companion object {
        const val RESOLVER_LIST_ID = "resolver_list"
        const val HEADER_ID = "chooser_header"
        val HEADER_SELECTOR = androidResSelector(HEADER_ID)
        const val UI_RESPONSE_TIMEOUT_MSECS = 3000
    }
}

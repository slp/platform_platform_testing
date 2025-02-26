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
import android.platform.test.scenario.tapl_common.Gestures
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat

/** System UI test automation object representing a notification hidden's menu (a.k.a. guts). */
class NotificationGuts internal constructor(private val mNotification: UiObject2) {
    /** Taps the "Done" button on the notification guts. */
    fun close() {
        val gutsSelector = sysuiResSelector(GUTS_ID)
        mNotification.assertVisibility(gutsSelector, visible = true)
        Gestures.click(mNotification.waitForObj(By.text("Done")), "Done button")
        assertThat(mNotification.wait(Until.gone(gutsSelector), UI_RESPONSE_TIMEOUT_MSECS)).isTrue()
    }

    private companion object {
        const val GUTS_ID = "notification_guts"
        const val UI_RESPONSE_TIMEOUT_MSECS: Long = 3000
    }
}

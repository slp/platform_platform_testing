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

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import androidx.test.uiautomator.By
import com.android.launcher3.tapl.LauncherInstrumentation
import java.util.regex.Pattern

/** System UI test automation object representing the panel with power controls aka "power menu". */
class PowerPanel internal constructor() {
    init {
        POWER_PANEL_SELECTOR.assertVisible()
        POWER_OFF_BUTTON_SELECTOR.assertVisible()
    }

    /** Closes the panel and returns to the previous UI object. */
    fun close() {
        LauncherInstrumentation().pressBack()
        POWER_PANEL_SELECTOR.assertInvisible()
        POWER_OFF_BUTTON_SELECTOR.assertInvisible { "System power menu is visible" }
    }

    private companion object {
        private val POWER_PANEL_SELECTOR = sysuiResSelector("list_flow")

        // https://hsv.googleplex.com/6244730548518912?node=13
        private val POWER_OFF_BUTTON_SELECTOR =
            By.text(Pattern.compile("Power off", Pattern.CASE_INSENSITIVE))
    }
}

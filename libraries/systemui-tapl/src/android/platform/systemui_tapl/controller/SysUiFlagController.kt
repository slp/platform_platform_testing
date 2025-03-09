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

package android.platform.systemui_tapl.controller

import android.platform.uiautomator_helpers.DeviceHelpers.shell
import android.platform.uiautomator_helpers.DeviceHelpers.uiDevice

private const val BASE_FLAG_COMMAND = "cmd statusbar flag"

class SysUiFlagController private constructor() {

    /** Sets the flag value to [newValue]. */
    fun setValue(flag: SystemUIFlag, newValue: String) {
        uiDevice.shell("${BASE_FLAG_COMMAND} ${flag.flagName} $newValue")
    }

    /** `True` when the flag is enabled. */
    fun isEnabled(flag: SystemUIFlag): Boolean =
        uiDevice.shell("$BASE_FLAG_COMMAND ${flag.flagName}").trim().endsWith("true")

    /** Resets the flag to the default value. */
    fun reset(flag: SystemUIFlag) {
        uiDevice.shell("$BASE_FLAG_COMMAND ${flag.flagName} erase")
    }

    /** List of SysUI flags used by tests. */
    enum class SystemUIFlag(val flagName: String) {
        FULL_SCREEN_USER_SWITCHER("full_screen_user_switcher")
    }

    companion object {
        fun get() = SysUiFlagController()
    }
}

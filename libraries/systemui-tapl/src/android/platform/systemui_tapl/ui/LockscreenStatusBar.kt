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
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import com.android.settingslib.flags.Flags.newStatusBarIcons
import org.junit.Assume.assumeFalse

/** StatusBar visible from the lockscreen. */
class LockscreenStatusBar internal constructor() {

    init {
        KEYGUARD_STATUS_BAR_SELECTOR.assertVisible { "Lockscreen statusbar not found." }
    }

    /**
     * Returns the value of the battery level on lockscreen statusbar. Experimental.
     *
     * TODO(b/328785514): this method will no longer work with the rollout of NEW_STATUS_BAR_ICONS
     */
    fun getBatteryLevel(): String {
        assumeFalse(newStatusBarIcons())
        return waitForObj(sysuiResSelector(StatusBar.BATTERY_LEVEL_TEXT_ID)) {
                "Battery percentage not found on lock screen statusbar."
            }
            .text
    }

    /** Gets user switcher chip. */
    val userSwitcherChip: UserSwitcherChip
        get() = UserSwitcherChip()

    /** Asserts that user switcher chip is invisible. */
    fun assertUserSwitcherChipIsInvisible() {
        sysuiResSelector(UserSwitcherChip.USER_SWITCHER_CONTAINER_ID).assertInvisible()
    }

    private companion object {
        // https://hsv.googleplex.com/6309313707507712?node=45
        val KEYGUARD_STATUS_BAR_SELECTOR = sysuiResSelector("keyguard_header")
    }
}

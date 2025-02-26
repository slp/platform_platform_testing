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
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.uiautomatorhelpers.WaitUtils.waitForValueToSettle

/**
 * A container on the status bar that can launch user selection panel.
 *
 * https://hsv.googleplex.com/5504620500615168?node=13
 */
class UserSwitcherChip internal constructor() {

    private val container =
        TaplUiDevice.waitForObject(
            sysuiResSelector(USER_SWITCHER_CONTAINER_ID),
            "User switcher chip",
        )

    /** Gets the current user name. */
    val username: String
        get() =
            container.waitForChildObject("current_user_name", "Current user's name").uiObject.text

    /** Opens the user selection panel by clicking user switcher chip. */
    fun clickToOpenUserSelector(): UserSelectionPanel {
        container.click()
        return UserSelectionPanel().also {
            waitForValueToSettle("User switch activity transition doesn't finish as expected.") {
                it.userContainerRect
            }
        }
    }

    companion object {
        const val USER_SWITCHER_CONTAINER_ID = "user_switcher_container"
    }
}

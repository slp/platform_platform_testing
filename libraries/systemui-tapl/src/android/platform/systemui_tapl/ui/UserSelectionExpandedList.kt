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

import android.platform.systemui_tapl.utils.DeviceUtils
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SYSUI_PACKAGE
import android.platform.systemui_tapl.utils.UserUtils.runThenWaitUntilSwitchCompleted
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until

/**
 * List that shows up from the bouncer user switcher.
 *
 * https://hsv.googleplex.com/6537215275433984?node=3
 */
class UserSelectionExpandedList internal constructor() {

    init {
        USERS_LIST_SELECTOR.assertVisible { "User selection list didn't appear" }
    }

    /** Opens add user prompt. */
    fun openAddUserPrompt(): AddUserPrompt {
        TaplUiDevice.waitForObject(ADD_USER_SELECTOR, "Add user menu item").click()
        return AddUserPrompt()
    }

    /** Selects switching to an existing user or a new guest user. */
    fun switchToUser(userName: String) {
        runThenWaitUntilSwitchCompleted {
            // (b/265080418): The clickable attribute of these items are always false.
            uiDevice.waitForObj(By.pkg(SYSUI_PACKAGE).text(userName)).click()
        }
    }

    /**
     * The number of options on the user selector list.
     * https://hsv.googleplex.com/6542084124180480?node=3
     */
    val numberOfUsers: Int
        get() {
            return uiDevice
                .wait(
                    Until.findObjects(sysuiResSelector(USER_SWITCHER_ITEM_ID)),
                    DeviceUtils.SHORT_WAIT.toMillis(),
                )
                ?.size ?: error("Can't find any user option.")
        }

    /** Asserts that user name exists on the expanded list. */
    fun assertUserNameInExpandedList(userName: String) {
        waitForObj(sysuiResSelector(USER_SWITCHER_ITEM_ID).hasDescendant(By.text(userName)))
    }

    private companion object {
        val IS_COMPOSE_BOUNCER_ENABLED =
            com.android.systemui.Flags.composeBouncer() ||
                com.android.systemui.Flags.sceneContainer()
        const val USER_SWITCHER_ITEM_ID = "user_switcher_item"

        // https://hsv.googleplex.com/6213668812357632?node=8
        val ADD_USER_SELECTOR: BySelector =
            sysuiResSelector(USER_SWITCHER_ITEM_ID).hasDescendant(By.text("Add user"))

        // https://hsv.googleplex.com/6542084124180480?node=6
        val OWNER_SELECTOR: BySelector =
            sysuiResSelector(USER_SWITCHER_ITEM_ID).hasDescendant(By.text("Owner"))

        // https://hsv.googleplex.com/6542084124180480?node=3
        val USERS_LIST_SELECTOR: BySelector =
            if (IS_COMPOSE_BOUNCER_ENABLED) sysuiResSelector("user_list_dropdown")
            else
                By.clazz("android.widget.ListView").pkg(SYSUI_PACKAGE).hasDescendant(OWNER_SELECTOR)
    }
}

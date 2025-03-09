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

import android.graphics.Rect
import android.platform.systemui_tapl.controller.SysUiFlagController
import android.platform.systemui_tapl.controller.SysUiFlagController.SystemUIFlag
import android.platform.systemui_tapl.utils.DeviceUtils.SHORT_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.UserUtils.runThenWaitUntilSwitchCompleted
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import java.util.regex.Pattern.compile

/**
 * Panel that shows up from the QS multiuser button or user switcher chip.
 *
 * Based on the [FULL_SCREEN_USER_SWITCHER] flag, this can be either:
 * - full screen: http://go/hsv/5243841595572224
 * - dialog: http://go/hsv/5008296081620992
 */
class UserSelectionPanel internal constructor() {

    private val fullscreen: Boolean = SystemUIFlag.FULL_SCREEN_USER_SWITCHER.isEnabled

    init {
        userSwitcherSelector().assertVisible { "UserSelectionPanel didn't appear" }
    }

    /** The number of active users on the current panel. Experimental. */
    val usersCount: Int
        get() {
            return uiDevice.wait(Until.findObjects(userSelector()), SHORT_WAIT.toMillis()).size
        }

    /**
     * The visible bound of the user container. It's mainly for screenshot testing. Experimental.
     */
    val userContainerRect: Rect?
        get() {
            if (fullscreen) {
                return uiDevice
                    .waitForObj(sysuiResSelector(FULLSCREEN_USER_CONTAINER_ID))
                    .waitForObj(sysuiResSelector("flow"))
                    .visibleBounds
            }
            throw NotImplementedError("userContainerRect isn't supported on the current device.")
        }

    /** Closes user selection. */
    fun close() {
        TaplUiDevice.waitForObject(closeSelector(), "Close button").click()
    }

    /** Opens user settings by clicking the User Settings button. */
    fun openUserSettings(): MultipleUsersSettings {
        if (fullscreen) {
            clickButtonFromAddMenu(MANAGE_USERS_BUTTON_LABEL)
        } else {
            // http://go/hsv/5905004487507968?node=28
            val userSettingsButton = androidResSelector("button3").text("Manage users")
            TaplUiDevice.waitForObject(userSettingsButton, "User settings button").click()
        }
        return MultipleUsersSettings()
    }

    /**
     * Unless guest user is initialized in some other way, on every UserSelection type we have
     * creating always means selecting guest user.
     */
    fun createAndSwitchToGuestUser() {
        runThenWaitUntilSwitchCompleted {
            if (fullscreen) {
                clickButtonFromAddMenu(ADD_GUEST_BUTTON_LABEL)
            } else {
                selectUser(GUEST_NAME)
            }
        }
    }

    /** Opens add user prompt. */
    fun openAddUserPrompt(): AddUserPrompt {
        if (fullscreen) {
            clickButtonFromAddMenu(ADD_USER_BUTTON_LABEL)
        } else {
            selectUser(ADD_USER_BUTTON_LABEL)
        }
        return AddUserPrompt()
    }

    /** Selects switching back to an already added guest user. */
    fun switchBackToGuest(): WelcomeBackGuestDialog {
        runThenWaitUntilSwitchCompleted { selectUser(GUEST_NAME) }
        return WelcomeBackGuestDialog()
    }

    /** Selects switching to an existing user or a new guest user. */
    fun switchToUser(userName: String) {
        runThenWaitUntilSwitchCompleted { selectUser(userName) }
    }

    /**
     * Selects an entry from the "Add" menu, available only in fullscreen.
     *
     * https://hsv.googleplex.com/5243841595572224?node=10
     */
    private fun clickButtonFromAddMenu(label: String) {
        fun clickButtonWithText(label: String) {
            // TODO(b/260815442): The clickable attribute of these items in the menu list is false.
            waitForObj(By.text(label)).click()
        }
        clickButtonWithText("Add")
        clickButtonWithText(label)
    }

    /** Selects the user with [userName]. */
    private fun selectUser(userName: String) {
        val userItemSelector: BySelector =
            if (fullscreen) By.clazz("android.view.ViewGroup") else sysuiResSelector("user_item")

        waitForObj(userItemSelector.hasDescendant(userSelector().text(userName))).click()
    }

    /** Selects exiting the guest user. */
    fun exitGuest(): ExitGuestPrompt {
        selectUser(EXIT_GUEST_ITEM)
        return ExitGuestPrompt()
    }

    private fun userSwitcherSelector(): BySelector =
        if (fullscreen) {
            sysuiResSelector("user_switcher_root")
        } else {
            androidResSelector("alertTitle").text("Select user")
        }

    private fun closeSelector(): BySelector =
        if (fullscreen) {
            // http://go/hsv/5243841595572224?node=9
            sysuiResSelector(FULLSCREEN_CANCEL_ID)
        } else {
            // http://go/hsv/5905004487507968?node=29
            androidResSelector("button1").text(compile("Close|Done"))
        }

    // http://go/hsv/5008296081620992?node=10#
    private fun userSelector(): BySelector =
        sysuiResSelector(if (fullscreen) "user_switcher_text" else "user_name")

    companion object {
        private const val GUEST_NAME = "Guest"
        private const val MANAGE_USERS_BUTTON_LABEL = "Manage users"
        private const val ADD_GUEST_BUTTON_LABEL = "Add guest"
        private const val ADD_USER_BUTTON_LABEL = "Add user"
        private const val FULLSCREEN_CANCEL_ID = "cancel"
        private const val EXIT_GUEST_ITEM = "Exit guest"
        private const val FULLSCREEN_USER_CONTAINER_ID = "user_switcher_grid_container"
    }

    private val SystemUIFlag.isEnabled: Boolean
        get() = SysUiFlagController.get().isEnabled(this)
}

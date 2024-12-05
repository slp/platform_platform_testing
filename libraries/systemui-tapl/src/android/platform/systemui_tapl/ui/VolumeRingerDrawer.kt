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

import android.platform.systemui_tapl.controller.VolumeController.RingerMode
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2

/**
 * Ringer drawer is a container which is opened after clicking ringer mode button.
 *
 * Volume dialog(drawer closed): https://hsv.googleplex.com/4762218357850112
 *
 * Ringer drawer: https://hsv.googleplex.com/5102770609717248
 */
class VolumeRingerDrawer internal constructor() {

    private val container: UiObject2

    init {
        val containerSel = sysuiResSelector("volume_drawer_container")
        this.container = waitForObj(containerSel) { "Can't find the ringer drawer." }
    }

    /**
     * Detect the current selected mode by checking the highlighted ringer icon. The highlighted
     * icon is the one with the active icon container on the top.
     */
    val selectedMode: RingerMode
        get() {
            val activeIconSel = sysuiResSelector("volume_new_ringer_active_icon_container")
            val activeIcon =
                waitForObj(activeIconSel) { "Can't find any active icon on the drawer." }
            val center = activeIcon.visibleCenter
            return RingerMode.values()
                .filter { it.isAvailable }
                .first {
                    val icon =
                        container.waitForObj(it.getIconSelector()) {
                            "Can't find $it icon on the drawer."
                        }
                    icon.visibleBounds.contains(center.x, center.y)
                }
        }

    /** Click the given ringer icon in the drawer. */
    fun selectRingerMode(mode: RingerMode): VolumeDialog {
        waitForObj(mode.getIconSelector()) { "$mode icon not found" }.click()
        return VolumeDialog()
    }

    private fun RingerMode.getIconSelector(): BySelector {
        return when (this) {
            RingerMode.NORMAL -> "volume_drawer_normal"
            RingerMode.SILENT -> "volume_drawer_mute"
            RingerMode.VIBRATE -> "volume_drawer_vibrate"
        }.let { sysuiResSelector(it) }
    }
}

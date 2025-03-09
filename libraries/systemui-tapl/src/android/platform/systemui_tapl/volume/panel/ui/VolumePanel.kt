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

package android.platform.systemui_tapl.volume.panel.ui

import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SYSUI_PACKAGE
import android.platform.uiautomator_helpers.DeviceHelpers.assertInvisible
import android.platform.uiautomator_helpers.DeviceHelpers.assertVisible
import android.platform.uiautomator_helpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2

/** Same as com.android.systemui.volume.panel.ui.composable.VolumePanelRoot#VolumePanelTestTag */
private const val VOLUME_PANEL_TEST_TAG = "VolumePanel"

/** Volume Panel components. */
class VolumePanel {

    init {
        sysuiResSelector(VOLUME_PANEL_TEST_TAG).assertVisible(timeout = LONG_WAIT)
    }

    private val container: UiObject2 =
        waitForObj(sysuiResSelector(VOLUME_PANEL_TEST_TAG)) { "Can't find volume panel" }

    /** Returns an access points for a particular slider for a [streamType] */
    fun slider(@SliderStream streamType: Int): VolumePanelSlider =
        VolumePanelSliderImpl(container, streamType)

    /** Clicks Done button */
    fun done() {
        container
            .waitForObj(By.pkg(SYSUI_PACKAGE).text("Done")) { "Can't find Done button" }
            .click()
        sysuiResSelector(VOLUME_PANEL_TEST_TAG).assertInvisible()
    }
}

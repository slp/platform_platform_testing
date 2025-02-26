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

import android.platform.systemui_tapl.utils.DeviceUtils.settingsResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import androidx.test.uiautomator.BySelector

/**
 * Page containing users.
 *
 * https://hsv.googleplex.com/6421165189890048
 */
class MultipleUsersSettings internal constructor() {
    init {
        PAGE_SELECTOR.assertVisible { "Multiple users settings page didn't appear" }
    }

    companion object {
        // https://hsv.googleplex.com/6421165189890048?node=7
        private val PAGE_SELECTOR: BySelector =
            settingsResSelector("collapsing_toolbar").desc("Users")
    }
}

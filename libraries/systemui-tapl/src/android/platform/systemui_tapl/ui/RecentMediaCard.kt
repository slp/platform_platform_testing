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
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForPossibleEmpty

/**
 * Recommend Media Card on the Universal Media Object.
 *
 * **See:** [HSV](https://hsv.googleplex.com/6195547053490176?node=86)
 */
class RecentMediaCard internal constructor() {

    init {
        sysuiResSelector("media_recommendations_updated").assertVisible {
            "Can't find recent media card."
        }
    }

    /**
     * The titles of recommended recent medias. The layout of the card is defined in
     * [RecommendationViewHolder].
     */
    val mediaTitles: List<String>
        get() = waitForPossibleEmpty(sysuiResSelector("media_title")).map { it.text }
}

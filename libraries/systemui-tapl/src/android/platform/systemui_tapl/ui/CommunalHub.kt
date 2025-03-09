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

import android.graphics.PointF
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.FLING_GESTURE_INTERPOLATOR
import androidx.test.uiautomator.By

/** System UI test automation object representing the communal hub. */
class CommunalHub internal constructor() {
    init {
        COMMUNAL_SELECTOR.assertVisible { "Communal Hub is not visible" }
    }

    /** Swipes right on the communal hub to exit the surface. */
    fun swipeRightToExit() {
        COMMUNAL_SELECTOR.assertVisible { "Communal Hub is not visible for swiping right" }
        swipeRight()
        COMMUNAL_SELECTOR.assertInvisible { "Communal Hub is still visible after swiping right" }
    }

    private fun swipeRight() {
        val bounds = waitForObj(COMMUNAL_SELECTOR).visibleBounds
        val swipeY = bounds.top + bounds.height() / 2f
        BetterSwipe.from(PointF(bounds.left + 1f, swipeY))
            .to(
                PointF(bounds.left + bounds.width() / 2f, swipeY),
                interpolator = FLING_GESTURE_INTERPOLATOR,
            )
            .release()
    }

    companion object {
        const val NAMESPACE_COMMUNAL = "communal"
        const val FLAG_COMMUNAL_HUB = "com.android.systemui.communal_hub"

        private const val COMMUNAL_HUB_RESOURCE_ID = "communal_hub"
        val COMMUNAL_SELECTOR = By.res(COMMUNAL_HUB_RESOURCE_ID)
    }
}

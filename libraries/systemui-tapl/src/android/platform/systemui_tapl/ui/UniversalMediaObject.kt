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

import android.platform.systemui_tapl.ui.NotificationShadeType.NORMAL
import android.platform.systemui_tapl.ui.NotificationShadeType.SPLIT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertWithMessage

/**
 * Represents the universal media object(UMO) displayed in the [NotificationShade]. UMO is not the
 * same as a media player. It contains a scrollable view called the Carousel, which can contain
 * multiple media players and recent media cards.
 */
class UniversalMediaObject internal constructor() {

    init {
        MEDIA_CAROUSEL_SCROLLER.assertVisible { "Media carousel not visible" }
    }

    /** Verifies that the media player is covering the entire space. */
    fun assertCoversEntireSpace() {
        val mediaPlayerWidth = mediaCarouselUiObject.visibleBounds.width()
        val deviceWidth = uiDevice.displayWidth

        when (NotificationShade().type!!) {
            NORMAL ->
                assertWithMessage("Media player in normal shade is not covering the entire space")
                    .that(mediaPlayerWidth)
                    .isGreaterThan(deviceWidth / 2)
            SPLIT ->
                // should fit slightly less than half of the screen (due to margins)
                assertWithMessage("Media player in split shade is not covering enough space")
                    .that(mediaPlayerWidth)
                    .isIn(Range.open(deviceWidth / 4 + 1, deviceWidth / 2))
        }
    }

    private val mediaCarouselUiObject: UiObject2
        get() = uiDevice.waitForObj(MEDIA_CAROUSEL_SCROLLER)

    companion object {
        val MEDIA_CAROUSEL_SCROLLER: BySelector = sysuiResSelector("media_carousel_scroller")
    }

    /** Get the recommend media card on the UMO, or fail if it's not visible */
    val recentMediaCard: RecentMediaCard
        get() = RecentMediaCard()
}

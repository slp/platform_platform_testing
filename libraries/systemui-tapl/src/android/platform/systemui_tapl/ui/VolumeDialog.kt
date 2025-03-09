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

import android.graphics.Point
import android.graphics.PointF
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.volume.panel.ui.VolumePanel
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import com.google.common.truth.Truth

/** System UI test automation object representing the dialog for adjusting the device volume. */
class VolumeDialog internal constructor() {
    init {
        assertVolumeDialogVisible()
    }

    /**
     * Changes the volume by dragging the volume slider.
     *
     * Note: Volume value cannot be more than 25 and less than 0.
     *
     * @param volume new volume value
     */
    fun setVolumeByDragging(volume: Int) {
        assertVolumeDialogVisible()
        dragAndChangeVolume(volume)
        assertVolumeDialogVisible()
    }

    /** Open the ringer drawer by clicking the ringer mode icon on the volume dialog. */
    fun openRingerDrawer(): VolumeRingerDrawer {
        val ringerIconSel = sysuiResSelector("volume_new_ringer_active_icon_container")
        waitForObj(ringerIconSel).click()
        return VolumeRingerDrawer()
    }

    /** Open the volume setting panel by clicking the setting icon on the volume dialog. */
    @Deprecated(
        "This new volume panel is rolled out. Use openNewVolumePanel instead",
        replaceWith = ReplaceWith("openNewVolumePanel"),
    )
    fun openVolumePanel(): VolumePanelDialog {
        sysuiResSelector("settings").click()
        return VolumePanelDialog()
    }

    /** Open the volume setting panel by clicking the setting icon on the volume dialog. */
    fun openNewVolumePanel(): VolumePanel {
        waitForObj(sysuiResSelector("settings")).click()
        return VolumePanel()
    }

    /**
     * Click the live caption button on the volume dialog.
     *
     * https://hsv.googleplex.com/4767031439130624
     *
     * @return this
     */
    fun toggleLiveCaptions(): VolumeDialog {
        waitForObj(sysuiResSelector("odi_captions_icon")).click()
        return this
    }

    companion object {
        private val SLIDER = sysuiResSelector("volume_row_slider")
        val PAGE_TITLE_SELECTOR = sysuiResSelector("volume_dialog")
        private const val MAX_VOLUME = 26
        private const val MIN_VOLUME = -1

        /**
         * Method used for dragging and changing the volume. Note: Volume value cannot be more than
         * 25 and less than 0.
         *
         * @param volume value for volume to changed
         */
        private fun dragAndChangeVolume(volume: Int) {
            val coordinates = getDragCoordinates(volume)
            assertVolumeDialogVisible()
            BetterSwipe.from(waitForObj(SLIDER).visibleCenter)
                .to(PointF(coordinates), interpolator = PRECISE_GESTURE_INTERPOLATOR)
                .release()
        }

        /** Asserts that the volume dialog is visible. */
        private fun assertVolumeDialogVisible() {
            PAGE_TITLE_SELECTOR.assertVisible()
        }

        /**
         * This will get the co-ordinate of the for volume slider based on the volume value
         * provided.
         *
         * Note: Volume value cannot be more than 25 and less than 0.
         *
         * Formula's used: Suppose volume slider length is 100 and volume provided is 15, therefore
         * slider should move to:
         *
         * FORMULA: (Volume Provided / Max Volume) * Slider Length Hence in current scenario its:
         * (15/25)*100
         *
         * Since the Android device coordinate works from top to down
         * [https://screenshot.googleplex .com/Zm3s1rqJ2Es], therefore formula used for coordinate
         * calculation is:
         *
         * X-Coordinate: TOP + ((MAX VOLUME - GIVEN VOLUME)/MAX VOLUME ) * (SLIDER BOTTOM Y
         * COORDINATE - SLIDER TOP Y COORDINATE)
         *
         * Y-Coordinate: SLIDER LEFT X COORDINATE + (SLIDER RIGHT X COORDINATE - SLIDER LEFT X
         * COORDINATE) / 2
         *
         * @param volume value for volume to changed
         * @return an of Point which is the coordinate of the slider to be moved too.
         */
        private fun getDragCoordinates(volume: Int): Point {
            Truth.assertThat(volume).isLessThan(MAX_VOLUME)
            Truth.assertThat(volume).isGreaterThan(MIN_VOLUME)
            val dimension = uiDevice.waitForObj(SLIDER).visibleBounds
            val top = dimension.top
            val left = dimension.left
            val right = dimension.right
            val bottom = dimension.bottom
            val y = (top + (25 - volume).toFloat() / 25 * (bottom - top)).toInt()
            val x = left + (right - left) / 2
            return Point(x, y)
        }
    }
}

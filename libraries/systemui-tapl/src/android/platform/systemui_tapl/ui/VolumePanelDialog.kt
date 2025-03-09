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

import android.media.AudioManager
import android.platform.systemui_tapl.utils.DeviceUtils.SHORT_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.settingsResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.betterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.context
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.widget.SeekBar
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertWithMessage
import java.util.regex.Pattern

/**
 * Volume settings panel which is opened after click setting button on the volume dialog.
 *
 * https://hsv.googleplex.com/5613609590718464?node=6
 */
class VolumePanelDialog internal constructor() {
    private val container: UiObject2 =
        waitForObj(settingsResSelector("panel_container")) { "Can't find volume menu dialog" }

    private val audioManager =
        context.getSystemService(AudioManager::class.java)
            ?: error("Unable to get the AudioManager for ${VolumePanelDialog::class}")

    /**
     * Convert volume index to the percentage.
     *
     * @param[stream] The volume stream to get the max/min volume.
     * @param[volumeIndex] The volume index to set.
     * @return The percentage of the max volume.
     */
    private fun volumeToPercentage(stream: Int, volumeIndex: Int): Float {
        val maxVol: Int = audioManager.getStreamMaxVolume(stream)
        val minVol: Int = audioManager.getStreamMinVolume(stream)
        assertWithMessage("input volume is of out range.")
            .that(volumeIndex)
            .isIn(Range.closed(minVol, maxVol))
        return (volumeIndex - minVol).toFloat() / (maxVol - minVol)
    }

    /**
     * Adjust ring volume by dragging "Ring & notification volume", or "Ring volume" slider.
     *
     * https://hsv.googleplex.com/5613609590718464?node=47
     *
     * @param[volume] The target volume index.
     */
    fun adjustRingVolume(volume: Int): VolumePanelDialog {
        val pattern = Pattern.compile("(Ring & notification volume)|(Ring volume)")
        val ringContainerSel = androidResSelector("content").hasDescendant(By.text(pattern))
        val ringContainer = container.waitForObj(ringContainerSel, SHORT_WAIT)
        val seekBar =
            ringContainer.waitForObj(By.clazz(SeekBar::class.java)) {
                "Can't find ring volume slider."
            }
        val bound = seekBar.visibleBounds
        val rate = volumeToPercentage(AudioManager.STREAM_RING, volume)
        val x = ((bound.right - bound.left) * rate + bound.left).toInt()
        uiDevice.betterSwipe(
            startX = bound.centerX(),
            startY = bound.centerY(),
            endX = x,
            endY = bound.centerY(),
            interpolator = PRECISE_GESTURE_INTERPOLATOR,
        )
        ensureThat("Volume is set to $volume.") {
            audioManager.getStreamVolume(AudioManager.STREAM_RING) == volume
        }
        return this
    }

    /**
     * Click done button.
     *
     * https://hsv.googleplex.com/5613609590718464?node=63
     */
    fun clickDone() {
        val btnSel = settingsResSelector("done")
        waitForObj(btnSel).click()
        btnSel.assertInvisible {
            "Volume panel dialog's done button should be invisible after clicking."
        }
    }
}

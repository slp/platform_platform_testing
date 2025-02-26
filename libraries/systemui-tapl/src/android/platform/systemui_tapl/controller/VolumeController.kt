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

package android.platform.systemui_tapl.controller

import android.Manifest.permission
import android.content.ContentResolver
import android.media.AudioManager
import android.os.VibratorManager
import android.platform.uiautomator_helpers.DeviceHelpers.context
import android.platform.uiautomator_helpers.ShellPrivilege
import android.platform.uiautomator_helpers.WaitUtils.ensureThat
import android.provider.Settings

/** Controller for adjusting the device volume. */
class VolumeController private constructor() {

    private val audioManager: AudioManager =
        context.getSystemService(AudioManager::class.java)
            ?: error("Can't get the AudioManager for ${VolumeController::class}}")

    /** Available ringer modes defined in [AudioManager] */
    enum class RingerMode(internal val mode: Int) {
        NORMAL(AudioManager.RINGER_MODE_NORMAL),
        SILENT(AudioManager.RINGER_MODE_SILENT),
        VIBRATE(AudioManager.RINGER_MODE_VIBRATE);

        /** Whether the ringer mode is available on the device under test. */
        val isAvailable: Boolean
            get() {
                return when (this) {
                    NORMAL,
                    SILENT -> true
                    VIBRATE -> hasVibrator
                }
            }
    }

    /**
     * Adjusts the volume to ADJUST_SAME.
     *
     * This method can be used to show the volume dialog.
     *
     * Note: ADJUST_SAME here means [AudioManager.ADJUST_SAME]. It tells AudioManager the direction
     * to change the volume, which is to keep the slider as is.
     */
    fun adjustVolumeSame() = adjustVolume(AudioManager.ADJUST_SAME)

    /** Adjusts the volume to ADJUST_LOWER. Shows the volume dialog. */
    fun adjustVolumeLower() = adjustVolume(AudioManager.ADJUST_LOWER)

    /** Adjusts the volume to ADJUST_RAISE. Shows the volume dialog. */
    fun adjustVolumeRaise() = adjustVolume(AudioManager.ADJUST_RAISE)

    private fun adjustVolume(direction: Int) {
        audioManager.adjustSuggestedStreamVolume(
            direction,
            AudioManager.STREAM_MUSIC,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    /**
     * The current Music stream's volume. This setter won't bring up volume dialog. Use
     * [adjustVolumeSame] instead if you want to open the dialog.
     */
    var volume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        set(volumeIndex) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volumeIndex,
                0, // Do nothing, prevent opening UI.
            )
            ensureThat("music volume == ${volumeIndex}") { volume == volumeIndex }
        }

    /**
     * Set SysUI's internal ringer mode state.
     *
     * Notice that it requires STATUS_BAR_SERVICE permission to complete the setup. If the caller
     * doesn't have the permission, the function will try to automatically grant the permission for
     * the caller. However, it may throw [SecurityException] if the caller has called
     * [android.app.UiAutomation.adoptShellPermissionIdentity] before. Therefore, use this function
     * carefully.
     *
     * @param ringerMode[RingerMode]
     */
    fun setRingerModeInternal(ringerMode: RingerMode) {
        ShellPrivilege(permission.STATUS_BAR_SERVICE).use {
            audioManager.ringerModeInternal = ringerMode.mode
        }
    }

    /**
     * Sets volume dialog timeout in ms.
     *
     * This method is called to set the timeout to a longer value to help the test to recognize its
     * visibility.
     *
     * Use [SetVolumeDialogTimeoutRule] in order to control the timeout value during the test only.
     *
     * @param cr content resolver.
     * @param timeout long press timeout.
     */
    fun setVolumeDialogTimeout(cr: ContentResolver, timeout: Int) {
        Settings.Secure.putInt(cr, Settings.Secure.VOLUME_DIALOG_DISMISS_TIMEOUT, timeout)
    }

    /** ringerMode is the current available Audio ringer mode. */
    val ringerMode: RingerMode
        get() {
            val ringerMode = audioManager.ringerMode
            return RingerMode.values().firstOrNull { it.mode == ringerMode }
                ?: error("Ringer mode $ringerMode isn't defined in ${RingerMode::class}")
        }

    /** Maximum volume of music stream. The value may be different based on the device's setting. */
    val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    /** Minimum volume of music stream. The value may be different based on the device's setting. */
    val minVolume: Int
        get() = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)

    companion object {
        @JvmStatic
        private val hasVibrator: Boolean
            get() =
                context
                    .getSystemService(VibratorManager::class.java)!!
                    .defaultVibrator
                    .hasVibrator()

        /** Returns an instance of VolumeController. */
        @JvmStatic
        fun get(): VolumeController {
            return VolumeController()
        }
    }
}

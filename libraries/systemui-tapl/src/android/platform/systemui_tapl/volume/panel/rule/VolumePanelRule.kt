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

package android.platform.systemui_tapl.volume.panel.rule

import android.media.AudioManager
import android.platform.systemui_tapl.ui.Root.Companion.get
import android.platform.systemui_tapl.volume.panel.ui.VolumePanel
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule that opens the Volume Panel for the duration of the test and ensures it's closed afterwards.
 */
class VolumePanelRule(private val mAudioManager: AudioManager) : TestWatcher() {

    /** Returns [VolumePanel] for easy access the opened panel. */
    val volumePanel: VolumePanel
        get() = checkNotNull(mutableVolumePanel) { "Volume Panel hasn't been opened yet" }

    private var mutableVolumePanel: VolumePanel? = null

    override fun starting(description: Description) {
        super.starting(description)
        // Open the volume dialog
        mAudioManager.adjustSuggestedStreamVolume(
            AudioManager.ADJUST_SAME,
            AudioManager.STREAM_MUSIC,
            AudioManager.FLAG_SHOW_UI,
        )
        mutableVolumePanel = get().volumeDialog.openNewVolumePanel()
    }

    override fun finished(description: Description) {
        mutableVolumePanel?.done()
        super.finished(description)
    }
}

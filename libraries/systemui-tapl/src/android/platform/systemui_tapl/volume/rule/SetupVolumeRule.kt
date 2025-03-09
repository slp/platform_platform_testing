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
package android.platform.systemui_tapl.volume.rule

import android.media.AudioManager
import android.platform.systemui_tapl.permissions.rule.AdoptShellPermissionsRule
import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Set volume for each provided stream for the duration of the test and reset them back to the
 * original values afterwards.
 */
class SetupVolumeRule(
    private val audioManager: AudioManager,
    private val testVolumes: Collection<Volume>,
) : TestWatcher() {

    constructor(
        audioManager: AudioManager,
        vararg testVolumes: Volume,
    ) : this(audioManager, testVolumes.toList())

    private val adoptShellPermissionRule =
        AdoptShellPermissionsRule(android.Manifest.permission.MANAGE_NOTIFICATIONS)

    private lateinit var originalVolumes: Collection<Volume>

    override fun apply(base: Statement?, description: Description?): Statement {
        return adoptShellPermissionRule.apply(super.apply(base, description), description)
    }

    override fun starting(description: Description) {
        super.starting(description)
        originalVolumes =
            testVolumes.map { Volume(it.audioStream, audioManager.getStreamVolume(it.audioStream)) }
        for (testVolume in testVolumes) {
            audioManager.setStreamVolume(testVolume.audioStream, testVolume.volume, 0)
        }
        Log.d("SetupVolumeRule", "Volumes set to: $testVolumes")
    }

    override fun finished(description: Description) {
        for (volume in originalVolumes) {
            audioManager.setStreamVolume(volume.audioStream, volume.volume, 0)
        }
        Log.d("SetupVolumeRule", "Volumes restored to: $originalVolumes")
        super.finished(description)
    }

    /** Models volume value for a stream. */
    data class Volume(val audioStream: Int, val volume: Int)
}

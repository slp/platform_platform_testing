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

import android.media.AudioManager
import androidx.annotation.IntDef

/** [AudioManager].STREAM_* supported by the [VolumePanelSlider]. */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_ALARM,
        ]
)
annotation class SliderStream {

    companion object {
        private val resourceIdsByStream =
            mapOf(
                AudioManager.STREAM_MUSIC to "Media",
                AudioManager.STREAM_VOICE_CALL to "Call",
                AudioManager.STREAM_RING to "Ring",
                AudioManager.STREAM_NOTIFICATION to "Notification",
                AudioManager.STREAM_ALARM to "Alarm",
            )

        fun getResourceId(@SliderStream stream: Int): String =
            resourceIdsByStream[stream] ?: throw IllegalArgumentException("Unknown stream $stream")
    }
}

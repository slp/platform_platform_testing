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

package platform.test.motion

import android.graphics.Bitmap
import platform.test.motion.filmstrip.Filmstrip
import platform.test.motion.filmstrip.MotionScreenshot
import platform.test.motion.filmstrip.VideoRenderer
import platform.test.motion.golden.TimeSeries

/**
 * The motion recorded while an animation is playing.
 *
 * @param timeSeries recorded time series.
 * @param screenshots screenshot of the animation per `frameId` in [timeSeries].
 */
class RecordedMotion
internal constructor(
    internal val testClassName: String,
    internal val testMethodName: String,
    val timeSeries: TimeSeries,
    screenshots: List<Bitmap>?,
) {
    /** Visual filmstrip of the animation. */
    val filmstrip: Filmstrip?
    /** Renders the screenshots as an MP4 video. */
    val videoRenderer: VideoRenderer?

    init {
        if (screenshots != null) {
            val motionScreenshots =
                timeSeries.frameIds.zip(screenshots) { frameId, bitmap ->
                    MotionScreenshot(frameId, bitmap)
                }
            filmstrip = Filmstrip(motionScreenshots)
            videoRenderer = VideoRenderer(motionScreenshots)
        } else {
            filmstrip = null
            videoRenderer = null
        }
    }

    companion object {
        fun MotionTestRule<*>.create(timeSeries: TimeSeries, screenshots: List<Bitmap>?) =
            RecordedMotion(
                checkNotNull(testClassName),
                checkNotNull(testMethodName),
                timeSeries,
                screenshots,
            )
    }
}

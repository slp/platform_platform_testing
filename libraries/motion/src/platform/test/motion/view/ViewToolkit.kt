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

package platform.test.motion.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.graphics.Bitmap
import android.view.View
import androidx.test.core.app.ActivityScenario
import com.google.errorprone.annotations.CheckReturnValue
import java.util.concurrent.TimeUnit
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.RecordedMotion.Companion.create
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.Feature
import platform.test.motion.golden.FrameId
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimeSeriesCaptureScope
import platform.test.motion.golden.TimestampFrameId
import platform.test.screenshot.BitmapSupplier
import platform.test.screenshot.captureToBitmapAsync

/** Toolkit class to use for View-based [MotionTestRule] tests. */
class ViewToolkit(internal val currentActivityScenario: () -> ActivityScenario<*>)

/**
 * Defines the sampling of features during a test run.
 *
 * @param captureRoot the root `observing` object, available in [timeSeriesCapture]'s
 *   [TimeSeriesCaptureScope].
 * @param sampling Sampling times at which to capture the animation.
 * @param visualCapture Supplies a screenshot per sampled frame.
 * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
 */
data class ViewRecordingSpec<T>(
    val captureRoot: T,
    val sampling: AnimationSampling,
    val visualCapture: BitmapSupplier?,
    val timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
) {
    companion object {
        /**
         * Creates at [ViewRecordingSpec] to sample `this` [T].
         *
         * Captures a screenshot on the [screenshotView].
         *
         * @param sampling Sampling times at which to capture the animation.
         * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
         */
        fun <T> T.capture(
            screenshotView: View,
            sampling: AnimationSampling,
            timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
        ) =
            ViewRecordingSpec(
                captureRoot = this,
                sampling = sampling,
                visualCapture = { screenshotView.captureToBitmapAsync().get(10, TimeUnit.SECONDS) },
                timeSeriesCapture,
            )

        /**
         * Creates a [ViewRecordingSpec] to sample `this` [View], and captures a screenshot thereof.
         *
         * @param sampling Sampling times at which to capture the animation.
         * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
         */
        fun <T : View> T.capture(
            sampling: AnimationSampling,
            timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
        ) =
            capture(
                screenshotView = this,
                sampling = sampling,
                timeSeriesCapture = timeSeriesCapture,
            )

        /**
         * Creates at [ViewRecordingSpec] to sample `this` [T].
         *
         * @param sampling Sampling times at which to capture the animation.
         * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
         */
        fun <T> T.captureWithoutScreenshot(
            sampling: AnimationSampling,
            timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
        ) =
            ViewRecordingSpec(
                captureRoot = this,
                sampling = sampling,
                visualCapture = null,
                timeSeriesCapture = timeSeriesCapture,
            )
    }
}

/** Runs the [animator] and records the time-series of the features specified in [recordingSpec]. */
@CheckReturnValue
fun <T> MotionTestRule<ViewToolkit>.record(
    animator: AnimatorSet,
    recordingSpec: ViewRecordingSpec<T>,
): RecordedMotion {
    require(!animator.isStarted) { "AnimatorSet must not have been started." }
    return recordSeekableAnimation(animator.makeTestable(), recordingSpec)
}

internal fun <T> MotionTestRule<ViewToolkit>.recordSeekableAnimation(
    seekableAnimation: SeekableAnimation,
    options: ViewRecordingSpec<T>,
): RecordedMotion {
    val visualCapture = options.visualCapture
    val sampling = options.sampling

    val activityScenario = toolkit.currentActivityScenario()
    val frameIdCollector = mutableListOf<FrameId>()
    val propertyCollector = mutableMapOf<String, MutableList<DataPoint<*>>>()
    val screenshotCollector = visualCapture?.let { mutableListOf<Bitmap>() }

    fun recordFrame(frameId: FrameId) {
        frameIdCollector.add(frameId)
        activityScenario.onActivity {
            with(options) {
                timeSeriesCapture.invoke(TimeSeriesCaptureScope(captureRoot, propertyCollector))
            }
        }

        screenshotCollector?.add(visualCapture())
    }

    if (sampling.sampleBefore) {
        recordFrame(SupplementalFrameId("before"))
    }

    for (sampleProgress in sampling.sampleAt) {
        lateinit var frameId: FrameId
        activityScenario.onActivity { frameId = seekableAnimation.seekTo(sampleProgress) }
        recordFrame(frameId)
    }

    if (sampling.sampleAfter) {
        seekableAnimation.seekTo(1f)
        recordFrame(SupplementalFrameId("after"))
    }

    val timeSeries =
        TimeSeries(
            frameIdCollector.toList(),
            propertyCollector.entries.map { entry -> Feature(entry.key, entry.value) },
        )

    return create(timeSeries, screenshotCollector)
}

internal interface SeekableAnimation {
    fun seekTo(progress: Float): FrameId
}

internal fun AnimatorSet.makeTestable(): SeekableAnimation {
    val seekableDuration = duration
    check(seekableDuration != Animator.DURATION_INFINITE)

    return object : SeekableAnimation {
        override fun seekTo(progress: Float): FrameId {
            currentPlayTime = (seekableDuration * progress).toLong()
            return TimestampFrameId(currentPlayTime)
        }
    }
}

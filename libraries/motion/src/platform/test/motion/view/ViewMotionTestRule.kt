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
import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import androidx.annotation.OptIn
import androidx.test.annotation.ExperimentalTestApi
import androidx.test.core.app.ActivityScenario
import com.google.errorprone.annotations.CheckReturnValue
import java.util.concurrent.TimeUnit
import platform.test.motion.MotionRecorder
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.Sampling
import platform.test.motion.TimeSeriesCaptureScope
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.Feature
import platform.test.motion.golden.FrameId
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimestampFrameId
import platform.test.screenshot.BitmapDiffer
import platform.test.screenshot.BitmapSupplier
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.captureToBitmapAsync

/**
 * Test rule to verify correctness of View-based animations.
 *
 * TODO(b/322324387): Decide whether using `ActivityScenario` is a good way for synchronizing
 *   between test and UI code.
 */
class ViewMotionTestRule<A : Activity>(
    goldenPathManager: GoldenPathManager,
    private val currentActivityScenario: () -> ActivityScenario<A>,
    bitmapDiffer: BitmapDiffer? = null,
) : MotionTestRule(goldenPathManager, bitmapDiffer) {

    @CheckReturnValue
    fun checkThat(animator: AnimatorSet): MotionRecorder {
        require(!animator.isStarted) { "AnimatorSet must not have been started." }
        return checkThat(animator.makeTestable())
    }

    @CheckReturnValue
    internal fun checkThat(seekableAnimation: SeekableAnimation) =
        object : MotionRecorder {
            override fun <T> record(
                captureRoot: T,
                sampling: Sampling,
                visualCapture: BitmapSupplier?,
                timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
            ) =
                recordMotion(
                    seekableAnimation,
                    captureRoot,
                    sampling,
                    visualCapture,
                    timeSeriesCapture
                )
        }

    internal fun <T> recordMotion(
        seekableAnimation: SeekableAnimation,
        captureScope: T,
        sampling: Sampling,
        visualCapture: BitmapSupplier?,
        timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
    ): RecordedMotion {
        val activityScenario = currentActivityScenario()
        val frameIdCollector = mutableListOf<FrameId>()
        val propertyCollector = mutableMapOf<String, MutableList<DataPoint<*>>>()
        val screenshotCollector = visualCapture?.let { mutableListOf<Bitmap>() }

        fun recordFrame(frameId: FrameId) {
            frameIdCollector.add(frameId)
            activityScenario.onActivity {
                timeSeriesCapture.invoke(TimeSeriesCaptureScope(captureScope, propertyCollector))
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
                propertyCollector.entries.map { entry -> Feature(entry.key, entry.value) }
            )

        return RecordedMotion(
            checkNotNull(testClassName),
            checkNotNull(testMethodName),
            timeSeries,
            screenshotCollector,
        )
    }
}

/**
 * Runs the animation and records the time-series of the specified features.
 *
 * Captures a screenshot of the [captureView] on each frame.
 *
 * @param captureRoot the root `observing` object, available in [timeSeriesCapture]'s
 *   [TimeSeriesCaptureScope].
 * @param captureView the view to capture each frame's screenshot.
 * @param sampling Sampling times at which to capture the animation.
 * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
 */
@OptIn(ExperimentalTestApi::class)
fun <T> MotionRecorder.record(
    captureRoot: T,
    captureView: View,
    sampling: Sampling,
    timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
) =
    record(
        captureRoot,
        sampling,
        visualCapture = { captureView.captureToBitmapAsync().get(10, TimeUnit.SECONDS) },
        timeSeriesCapture
    )

/**
 * Runs the animation and records the time-series of the specified features.
 *
 * Captures a screenshot on the [captureScope].
 *
 * @param captureRoot the root `observing` view, available in [timeSeriesCapture]'s
 *   [TimeSeriesCaptureScope], and the view to capture each frame's screenshot.
 * @param sampling Sampling times at which to capture the animation.
 * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
 */
@OptIn(ExperimentalTestApi::class)
fun <T : View> MotionRecorder.record(
    captureRoot: T,
    sampling: Sampling,
    timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
) = record(captureRoot, captureView = captureRoot, sampling, timeSeriesCapture)

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

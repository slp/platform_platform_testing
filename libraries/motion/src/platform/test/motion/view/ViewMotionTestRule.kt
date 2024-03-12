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
import platform.test.motion.FeatureCapture
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.Sampling
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.DataPointTypes
import platform.test.motion.golden.Feature
import platform.test.motion.golden.FrameId
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimestampFrameId
import platform.test.screenshot.BitmapDiffer
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.captureToBitmapAsync

/**
 * Test rule to verify correctness of View-based animations.
 *
 * TODO(b/322324387): Decide whether using `ActivityScenario` is a good way for synchronizing
 *   between test and UI code.
 */
@OptIn(ExperimentalTestApi::class)
class ViewMotionTestRule<A : Activity>(
    goldenPathManager: GoldenPathManager,
    private val currentActivityScenario: () -> ActivityScenario<A>,
    extraGoldenDataPointTypes: List<DataPointType<Any>> = emptyList(),
    bitmapDiffer: BitmapDiffer? = null,
) :
    MotionTestRule(
        goldenPathManager,
        defaultGoldenValueTypes + extraGoldenDataPointTypes,
        bitmapDiffer
    ) {

    @CheckReturnValue
    fun checkThat(animator: AnimatorSet): ViewMotionRecorder {
        require(!animator.isStarted) { "AnimatorSet must not have been started." }
        return checkThat(animator.makeTestable())
    }

    @CheckReturnValue
    internal fun checkThat(seekableAnimation: SeekableAnimation) =
        object : ViewMotionRecorder {
            override fun <T : View> record(
                onRootView: T,
                sampling: Sampling,
                captureValues: ViewFeatureCaptureScope<T>.() -> Unit
            ) = recordMotion(seekableAnimation, onRootView, sampling, captureValues)
        }

    internal fun <T : View> recordMotion(
        seekableAnimation: SeekableAnimation,
        rootView: T,
        sampling: Sampling,
        captureValues: ViewFeatureCaptureScope<T>.() -> Unit,
    ): RecordedMotion {
        val activityScenario = currentActivityScenario()
        val frameIdCollector = mutableListOf<FrameId>()
        val propertyCollector = mutableMapOf<String, MutableList<DataPoint<*>>>()
        val screenshotCollector = mutableListOf<Bitmap>()

        fun recordFrame(frameId: FrameId) {
            frameIdCollector.add(frameId)
            activityScenario.onActivity {
                captureValues.invoke(ViewFeatureCaptureScopeImpl(rootView, propertyCollector))
            }

            screenshotCollector.add(rootView.captureToBitmapAsync().get(10, TimeUnit.SECONDS))
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

        return RecordedMotion(
            checkNotNull(testClassName),
            checkNotNull(testMethodName),
            TimeSeries(
                frameIdCollector.toList(),
                propertyCollector.entries.map { entry -> Feature(entry.key, entry.value) }
            ),
            screenshotCollector,
        )
    }

    companion object {
        val defaultGoldenValueTypes = DataPointTypes.allTypes + ViewDataPointTypes.allTypes
    }
}

/** Intermediate helper interface to write fluent test assertions. */
@CheckReturnValue
interface ViewMotionRecorder {
    fun <T : View> record(
        onRootView: T,
        sampling: Sampling,
        captureValues: ViewFeatureCaptureScope<T>.() -> Unit,
    ): RecordedMotion
}

internal class ViewFeatureCaptureScopeImpl<T : View>(
    private val view: T?,
    private val valueCollector: MutableMap<String, MutableList<DataPoint<*>>>,
) : ViewFeatureCaptureScope<T> {
    override fun <U : View> onViewWithId(viewId: Int): ViewFeatureCaptureScope<U> {
        return ViewFeatureCaptureScopeImpl(view?.findViewById(viewId), valueCollector)
    }

    override fun <U : View> onView(resolveView: (T) -> U?): ViewFeatureCaptureScope<U> {
        return ViewFeatureCaptureScopeImpl(view?.let(resolveView), valueCollector)
    }

    override fun capture(viewCapture: FeatureCapture<in T, *>, name: String) {
        valueCollector
            .computeIfAbsent(name) { mutableListOf() }
            .add(if (view != null) viewCapture.capture(view) else DataPoint.notFound())
    }
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

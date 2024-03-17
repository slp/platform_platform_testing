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

import com.google.errorprone.annotations.CheckReturnValue
import platform.test.motion.golden.DataPoint
import platform.test.screenshot.BitmapSupplier

/**
 * Intermediate interface to configure and trigger the recoding of the motion.
 * - See [MotionTestRule]'s `checkThat` style-methods to obtain a [MotionRecorder].
 * - See environment-specific extension functions on [MotionRecorder] for easier-to-use overloads.
 */
@CheckReturnValue
interface MotionRecorder {

    /**
     * Runs the animation and records the time-series of the specified features.
     *
     * @param captureRoot the root `observing` object, available in [timeSeriesCapture]'s
     *   [TimeSeriesCaptureScope].
     * @param sampling Sampling times at which to capture the animation.
     * @param visualCapture provides a screenshot for each frame. When `null`, debug screenshots and
     *   filmstrip golden images are not available.
     * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
     */
    fun <T> record(
        captureRoot: T,
        sampling: Sampling,
        visualCapture: BitmapSupplier?,
        timeSeriesCapture: TimeSeriesCaptureScope<T>.() -> Unit,
    ): RecordedMotion
}

/**
 * Scope on which to define the features of the recorded a time-series.
 *
 * @param observing the object on which [capture] operates.
 */
class TimeSeriesCaptureScope<T>(
    private val observing: T?,
    private val valueCollector: MutableMap<String, MutableList<DataPoint<*>>>,
) {
    /**
     * Records a property from [observing], extracted by [featureCapture] and stored in the golden
     * as [name].
     *
     * If the backing [observing] object cannot be resolved during an animation frame,
     * `DataPoint.notFound` is recorded in the time-series.
     *
     * @param featureCapture extracts a [DataPoint] from [observing]
     * @param name unique, human-readable label under which the feature is stored in the time-series
     */
    fun capture(featureCapture: FeatureCapture<in T, *>, name: String) {
        val dataPoint =
            if (observing != null) featureCapture.capture(observing) else DataPoint.notFound()
        valueCollector.computeIfAbsent(name) { mutableListOf() }.add(dataPoint)
    }

    /**
     * Captures features on other, related objects.
     *
     * @param resolveRelated finds the related object on which to capture features, invoked once per
     *   animation frame. Can return null if the related object is not currently available in the
     *   scene.
     * @param nestedTimeSeriesCapture captures features on the related object.
     */
    fun <U> on(
        resolveRelated: (T) -> U?,
        nestedTimeSeriesCapture: TimeSeriesCaptureScope<U>.() -> Unit
    ) {
        with(TimeSeriesCaptureScope(observing?.let(resolveRelated), valueCollector)) {
            nestedTimeSeriesCapture()
        }
    }
}

/**
 * Captures a time-series feature of an observed [T].
 *
 * A [DataPoint] of type [V] is recorded at each frame.
 */
fun interface FeatureCapture<T, V : Any> {
    fun capture(observed: T): DataPoint<V>
}

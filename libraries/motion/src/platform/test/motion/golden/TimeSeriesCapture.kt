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

package platform.test.motion.golden

/**
 * Captures a time-series feature of an observed [T].
 *
 * A [DataPoint] of type [V] is recorded at each frame.
 */
class FeatureCapture<T, V : Any>(
    val name: String,
    private val captureFn: (T) -> DataPoint<V>,
) {
    fun capture(observed: T) = captureFn(observed)
}

class TimeSeriesCaptureScope<T>(
    private val observing: T?,
    private val valueCollector: MutableMap<String, MutableList<DataPoint<*>>>,
) {

    /**
     * Records a [DataPoint] from [observing], extracted [using] the specified [FeatureCapture] and
     * stored in the time-series as [name].
     *
     * If the backing [observing] object cannot be resolved during an animation frame,
     * `DataPoint.notFound` is recorded in the time-series.
     *
     * @param using extracts a [DataPoint] from [observing]
     * @param name unique, human-readable label under which the feature is stored in the time-series
     */
    fun feature(using: FeatureCapture<in T, *>, name: String = using.name) {
        val dataPoint = if (observing != null) using.capture(observing) else DataPoint.notFound()
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

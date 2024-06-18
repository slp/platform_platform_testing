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

import android.graphics.Point
import android.view.View
import platform.test.motion.golden.FeatureCapture
import platform.test.motion.golden.TimeSeriesCaptureScope
import platform.test.motion.golden.asDataPoint

/** Returns a [TimeSeriesCaptureScope] for the child view with the specified ID. */
fun <U : View> TimeSeriesCaptureScope<out View>.onViewWithId(
    viewId: Int,
    nestedTimeSeriesCapture: TimeSeriesCaptureScope<U>.() -> Unit
) = on({ it.findViewById(viewId) }, nestedTimeSeriesCapture)

/** Common, generic [FeatureCapture] implementations for Views. */
object ViewFeatureCaptures {
    /** Captures the `alpha` value of a view. */
    val alpha = FeatureCapture<View, Float>("alpha") { view -> view.alpha.asDataPoint() }

    /** Captures the `elevation` value of a view. */
    val elevation =
        FeatureCapture<View, Float>("elevation") { view -> view.elevation.asDataPoint() }

    /** Captures the `x` value of a view. */
    val x = FeatureCapture<View, Float>("x") { view -> view.x.asDataPoint() }

    /** Captures the `y` value of a view. */
    val y = FeatureCapture<View, Float>("y") { view -> view.y.asDataPoint() }

    /** Captures the top-left coordinate of the view in screen coordinate system. */
    val positionOnScreen =
        FeatureCapture<View, Point>("pos") { view ->
            val outLocation = IntArray(2)
            view.getLocationOnScreen(outLocation)

            Point()
                .apply {
                    x = outLocation[0]
                    y = outLocation[1]
                }
                .asDataPoint()
        }
}

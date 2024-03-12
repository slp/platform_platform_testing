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
import platform.test.motion.FeatureCapture
import platform.test.motion.golden.asDataPoint

/** A scope to allow recording properties on the view hierarchy. */
interface ViewFeatureCaptureScope<T : View> {

    /** Returns a [ViewFeatureCaptureScope] for the child view with the specified ID. */
    fun <U : View> onViewWithId(viewId: Int): ViewFeatureCaptureScope<U>

    /** Returns a [ViewFeatureCaptureScope] for the view . */
    fun <U : View> onView(resolveView: (T) -> U?): ViewFeatureCaptureScope<U>

    /**
     * Records a property from this scope, extracted by [viewCapture] and stored in the golden as
     * [name].
     *
     * Note that the [name] must be unique within a golden frame. If the view in this scope cannot
     * be resolved during an animation frame, `ViewNotFound.VIEW_NOT_FOUND` is recorded in the
     * golden.
     */
    fun capture(viewCapture: FeatureCapture<in T, *>, name: String)
}

/** Common, generic [FeatureCapture] implementations for Views. */
object ViewFeatureCaptures {
    /** Captures the `alpha` value of a view. */
    val alpha = FeatureCapture<View, Float> { view -> view.alpha.asDataPoint() }

    /** Captures the `elevation` value of a view. */
    val elevation = FeatureCapture<View, Float> { view -> view.elevation.asDataPoint() }

    /** Captures the `x` value of a view. */
    val x = FeatureCapture<View, Float> { view -> view.x.asDataPoint() }

    /** Captures the `y` value of a view. */
    val y = FeatureCapture<View, Float> { view -> view.y.asDataPoint() }

    /** Captures the top-left coordinate of the view in screen coordinate system. */
    val positionOnScreen =
        FeatureCapture<View, Point> { view ->
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

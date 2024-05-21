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

package platform.test.motion.compose

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import platform.test.motion.compose.values.MotionTestValues
import platform.test.motion.golden.FeatureCapture
import platform.test.motion.golden.TimeSeriesCaptureScope
import platform.test.motion.golden.asDataPoint

/** Common, generic [FeatureCapture] implementations for Compose. */
object ComposeFeatureCaptures {
    /** Size of a node in pixels. */
    val size = FeatureCapture<SemanticsNode, IntSize>("size") { it.size.asDataPoint() }
    /** Size of a node in DPs. */
    val dpSize =
        FeatureCapture<SemanticsNode, DpSize>("size") {
            with(it.layoutInfo.density) { it.size.toSize().toDpSize().asDataPoint() }
        }
    /**
     * The position of this node relative to the root of this Compose hierarchy, with no clipping
     * applied.
     */
    val positionInRoot =
        FeatureCapture<SemanticsNode, DpOffset>("position") {
            with(it.layoutInfo.density) {
                DpOffset(it.positionInRoot.x.toDp(), it.positionInRoot.y.toDp()).asDataPoint()
            }
        }

    /**
     * Captures the `alpha` value of a node.
     *
     * IMPORTANT: the alpha-value can only be captured if it has been exported in the production
     * code, see [MotionTestValues.alpha]
     */
    val alpha =
        FeatureCapture<SemanticsNode, Float>("alpha") {
            it.config[MotionTestValues.alpha.semanticsPropertyKey].asDataPoint()
        }
}

/**
 * Captures the feature using [capture] from a node found via [matcher].
 *
 * If zero or more than one matching node is found, `DataPoint.notFound()` is recorded.
 */
fun TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.feature(
    matcher: SemanticsMatcher,
    capture: FeatureCapture<SemanticsNode, *>,
    name: String = capture.name,
) {
    on({
        try {
            it.onNode(matcher).fetchSemanticsNode()
        } catch (e: AssertionError) {
            null
        }
    }) {
        feature(capture, name)
    }
}

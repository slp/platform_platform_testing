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
import platform.test.motion.compose.values.MotionTestValueKey
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.FeatureCapture
import platform.test.motion.golden.TimeSeriesCaptureScope

/** Matches nodes that have the [motionTestValueKey] set. */
fun <T> hasMotionTestValue(motionTestValueKey: MotionTestValueKey<T>): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(motionTestValueKey.semanticsPropertyKey)

/**
 * Looks up a node matching [matcher], and returns the associated [motionTestValueKey].
 *
 * [AssertionError] is thrown if zero or more than one matching node is found.
 * [IllegalStateException] is thrown if the node does not have a [motionTestValueKey] exported.
 */
fun <T> SemanticsNodeInteractionsProvider.motionTestValueOfNode(
    motionTestValueKey: MotionTestValueKey<T>,
    matcher: SemanticsMatcher = hasMotionTestValue(motionTestValueKey),
    useUnmergedTree: Boolean = false
): T = onNode(matcher, useUnmergedTree).fetchSemanticsNode().get(motionTestValueKey)

/**
 * Records a feature by sampling the value associated to [motionTestValueKey], on a single node
 * matching [matcher].
 *
 * Records `DataPoint.notFound()` if 0 or 2+ matching node are found. [IllegalStateException] is
 * thrown if the node does not have a [motionTestValueKey] exported.
 */
fun <T : Any> TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.feature(
    motionTestValueKey: MotionTestValueKey<T>,
    dataPointType: DataPointType<T>,
    matcher: SemanticsMatcher = hasMotionTestValue(motionTestValueKey),
    name: String = motionTestValueKey.semanticsPropertyKey.name
) {
    feature(
        matcher,
        FeatureCapture(name) { dataPointType.makeDataPoint(it.get(motionTestValueKey)) },
    )
}

/**
 * Captures the feature using [capture], where [capture]'s input is the value associated to
 * [motionTestValueKey], read from a single node matching [matcher].
 *
 * Records `DataPoint.notFound()` if 0 or 2+ matching node are found. [IllegalStateException] is
 * thrown if the node does not have a [motionTestValueKey] exported.
 */
fun <T> TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.feature(
    motionTestValueKey: MotionTestValueKey<T>,
    capture: FeatureCapture<T, *>,
    matcher: SemanticsMatcher = hasMotionTestValue(motionTestValueKey),
    name: String = "${motionTestValueKey.semanticsPropertyKey.name}_${capture.name}"
) {
    feature(
        matcher,
        FeatureCapture(name) { capture.capture(it.get(motionTestValueKey)) },
    )
}

private fun <T> SemanticsNode.get(motionTestValueKey: MotionTestValueKey<T>) =
    config[motionTestValueKey.semanticsPropertyKey]

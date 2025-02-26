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

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.json.JSONObject
import platform.test.motion.compose.DataPointTypes.intOffset
import platform.test.motion.compose.DataPointTypes.intSize
import platform.test.motion.compose.TransitionFeatureCapture.animatedVisibility
import platform.test.motion.compose.TransitionFeatureCapture.animatedVisibilityTransitions
import platform.test.motion.compose.values.MotionTestValueKey
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.DataPointTypes.float
import platform.test.motion.golden.FeatureCapture
import platform.test.motion.golden.NotFoundDataPoint
import platform.test.motion.golden.TimeSeriesCaptureScope
import platform.test.motion.golden.UnknownTypeException

/**
 * Captures animations started by the built-in `Transition.AnimatedContent(...)` transitions.
 *
 * Note: `Transition.AnimatedContent(...)` does not support providing a transition label currently.
 * The `getTransitionLabel` is intended as a stop-gap for providing this label after the fact, and
 * will be removed once the compose API is updated.
 *
 * @param getTransitionLabel Returns a label for the n-th AnimatedVisibility transition. The index
 *   represents the compose Order
 */
fun <T> TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.animatedVisibilityFeatures(
    transitionValueKey: MotionTestValueKey<Transition<T>>,
    matcher: SemanticsMatcher = hasMotionTestValue(transitionValueKey),
    name: String = transitionValueKey.semanticsPropertyKey.name,
    getTransitionLabel: (index: Int) -> String = { "AnimatedVisibility[$it]" },
) {
    on({
        try {
            it.onNode(matcher).fetchSemanticsNode().config[transitionValueKey.semanticsPropertyKey]
        } catch (e: AssertionError) {
            null
        }
    }) {
        feature(
            FeatureCapture(name) { rootTransition ->
                DataPoint.of(
                    buildMap {
                        val enterExitTransitions =
                            rootTransition.transitions.filterEnterExitTransitions()

                        enterExitTransitions
                            .filterByDirection(
                                source = EnterExitState.PreEnter,
                                target = EnterExitState.Visible,
                            )
                            .forEachIndexed { index, transition ->
                                val transitionLabel = "${getTransitionLabel(index)}::Enter"

                                put(transitionLabel, animatedVisibility.capture(transition))
                            }
                        enterExitTransitions
                            .filterByDirection(
                                source = EnterExitState.Visible,
                                target = EnterExitState.PostExit,
                            )
                            .forEachIndexed { index, transition ->
                                val transitionLabel = "${getTransitionLabel(index)}::Exit"

                                put(transitionLabel, animatedVisibility.capture(transition))
                            }
                    },
                    animatedVisibilityTransitions,
                )
            }
        )
    }
}

fun <T> ComposeRecordingSpec.Companion.duringTransition(
    transitionValueKey: MotionTestValueKey<Transition<T>>,
    recordBefore: Boolean = true,
    recordAfter: Boolean = true,
    timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
): ComposeRecordingSpec {
    return ComposeRecordingSpec(
        motionControl =
            MotionControl {
                awaitCondition { motionTestValueOfNode(transitionValueKey).isRunning }
                awaitCondition { !motionTestValueOfNode(transitionValueKey).isRunning }
            },
        recordBefore,
        recordAfter,
        timeSeriesCapture,
    )
}

@SuppressWarnings("unchecked_cast")
private fun List<Transition<*>>.filterEnterExitTransitions(): List<Transition<EnterExitState>> =
    filter {
            // http://go/cs-compose/symbol/AnimatedEnterExitImpl
            it.label?.endsWith("> EnterExitTransition") == true && it.currentState is EnterExitState
        }
        .map { it as Transition<EnterExitState> }

private fun List<Transition<EnterExitState>>.filterByDirection(
    source: EnterExitState,
    target: EnterExitState,
) = filter { it.currentState == source && it.targetState == target }

internal typealias AnimatedVisibilityTransitions = Map<String, DataPoint<AnimatedVisibilityValues>>

internal data class AnimatedVisibilityValues(
    val alpha: DataPoint<Float>,
    val slide: DataPoint<IntOffset>,
    val scale: DataPoint<Float>,
    val size: DataPoint<IntSize>,
)

internal object TransitionFeatureCapture {
    val animatedVisibilityTransitions: DataPointType<AnimatedVisibilityTransitions> =
        DataPointType(
            "animatedVisibilityTransitions",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    buildMap {
                        for (key in keys()) {
                            put(key, animatedVisibilityValues.fromJson(it.get(key)))
                        }
                    }
                }
            },
            valueToJson = {
                JSONObject().apply { it.forEach { (key, value) -> put(key, value.asJson()) } }
            },
        )

    val animatedVisibilityValues: DataPointType<AnimatedVisibilityValues> =
        DataPointType(
            "animatedVisibilityValues",
            jsonToValue = { json ->
                with(json as? JSONObject ?: throw UnknownTypeException()) {
                    val alpha =
                        if (json.has(alphaProperty)) float.fromJson(json.get(alphaProperty))
                        else DataPoint.notFound()
                    val slide =
                        if (json.has(slideProperty)) intOffset.fromJson(json.get(slideProperty))
                        else DataPoint.notFound()
                    val scale =
                        if (json.has(scaleProperty)) float.fromJson(json.get(scaleProperty))
                        else DataPoint.notFound()
                    val size =
                        if (json.has(sizeProperty)) intSize.fromJson(json.get(sizeProperty))
                        else DataPoint.notFound()

                    AnimatedVisibilityValues(alpha, slide, scale, size)
                }
            },
            valueToJson = { value ->
                JSONObject().apply {
                    value.alpha
                        .takeIf { it !is NotFoundDataPoint }
                        ?.also { put(alphaProperty, it.asJson()) }
                    value.slide
                        .takeIf { it !is NotFoundDataPoint }
                        ?.also { put(slideProperty, it.asJson()) }
                    value.scale
                        .takeIf { it !is NotFoundDataPoint }
                        ?.also { put(scaleProperty, it.asJson()) }
                    value.size
                        .takeIf { it !is NotFoundDataPoint }
                        ?.also { put(sizeProperty, it.asJson()) }
                }
            },
        )

    val animatedVisibility =
        FeatureCapture<Transition<EnterExitState>, AnimatedVisibilityValues>(
            "Animated Visibility"
        ) { transition ->
            var alpha = DataPoint.notFound<Float>()
            var slide = DataPoint.notFound<IntOffset>()
            var scale = DataPoint.notFound<Float>()
            var size = DataPoint.notFound<IntSize>()

            // Built-in animations are created in these functions:
            // http://go/cs-compose/symbol/createModifier/file:EnterExitTransition
            // http://go/cs-compose/symbol/createGraphicsLayerBlock/file:EnterExitTransition

            transition.animations.forEach {
                when (it.label) {
                    "Built-in alpha" -> alpha = DataPoint.of(it.value as Float?, float)
                    "Built-in slide" -> slide = DataPoint.of(it.value as IntOffset?, intOffset)
                    "Built-in scale" -> scale = DataPoint.of(it.value as Float?, float)
                    "Built-in shrink/expand" -> size = DataPoint.of(it.value as IntSize?, intSize)
                }
            }

            DataPoint.of(
                AnimatedVisibilityValues(alpha, slide, scale, size),
                animatedVisibilityValues,
            )
        }

    private const val alphaProperty = "alpha"
    private const val slideProperty = "slide"
    private const val scaleProperty = "scale"
    private const val sizeProperty = "size"
}

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

/**
 * Description of animation sampling strategy.
 *
 * @param sampleAt The animation progress fractions at which to capture an animation frame.
 * @param sampleBefore Samples the frame before the animation is started.
 * @param sampleAfter Samples the frame after the animation has ended.
 */
data class AnimationSampling(
    val sampleAt: List<Float>,
    val sampleBefore: Boolean = true,
    val sampleAfter: Boolean = true,
) {

    init {
        check(sampleAt.all { it in 0.0f..1.0f })
        check(sampleAt.zipWithNext().all { it.first < it.second })
    }

    companion object {
        /**
         * Creates a [AnimationSampling] to sample an animation exactly [sampleCount] times, evenly
         * distributed over the animations playtime.
         *
         * [sampleAtStart] and [sampleAtEnd] define whether a frame is sampled at progress 0 and 1,
         * respectively.
         *
         * [sampleBefore] and [sampleAfter] define whether to capture a sample before the animation
         * is started, or after it is finished respectively. This is helpful to capture issues
         * caused by code triggered at the start or end of the animation.
         *
         * [sampleAfter] is `false` by default, since [sampleAtEnd] in most cases equal to
         * [sampleAfter], as the animation is automatically ended when the playTime of the animation
         * equals the duration.
         */
        fun evenlySampled(
            sampleCount: Int,
            sampleBefore: Boolean = true,
            sampleAtStart: Boolean = true,
            sampleAtEnd: Boolean = true,
            sampleAfter: Boolean = !sampleAtEnd,
        ): AnimationSampling {
            if (sampleAtStart && sampleAtEnd) {
                require(sampleCount >= 2)
            } else {
                require(sampleCount >= 1)
            }

            val offset = if (sampleAtStart) 0 else 1
            val divider =
                when {
                    sampleAtStart xor sampleAtEnd -> sampleCount
                    sampleAtStart and sampleAtEnd -> sampleCount - 1
                    else -> sampleCount + 1
                }

            return AnimationSampling(
                List(sampleCount) { (1f / divider) * (it + offset) },
                sampleBefore,
                sampleAfter,
            )
        }
    }
}

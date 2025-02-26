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

package android.tools.traces.component

import android.tools.traces.surfaceflinger.Layer
import android.tools.traces.wm.Activity
import android.tools.traces.wm.WindowContainer
import java.util.function.Predicate

class SurfaceViewBackgroundMatcher : IComponentMatcher {
    /** {@inheritDoc} */
    override fun windowMatchesAnyOf(windows: Collection<WindowContainer>): Boolean {
        // Doesn't have a window component only layers
        return false
    }

    /** {@inheritDoc} */
    override fun activityMatchesAnyOf(activities: Collection<Activity>): Boolean {
        // Doesn't have a window component only layers
        return false
    }

    /** {@inheritDoc} */
    override fun layerMatchesAnyOf(layers: Collection<Layer>): Boolean {
        return layers.any { it.name.matches(Regex("^Background for \\w+ SurfaceView.*$")) }
    }

    /** {@inheritDoc} */
    override fun check(
        layers: Collection<Layer>,
        condition: Predicate<Collection<Layer>>,
    ): Boolean = condition.test(layers.filter { layerMatchesAnyOf(it) })

    /** {@inheritDoc} */
    override fun toActivityIdentifier(): String {
        throw NotImplementedError(
            "toActivityIdentifier() is not implemented on SurfaceViewBackgroundMatcher"
        )
    }

    /** {@inheritDoc} */
    override fun toWindowIdentifier(): String {
        throw NotImplementedError(
            "toWindowName() is not implemented on SurfaceViewBackgroundMatcher"
        )
    }

    /** {@inheritDoc} */
    override fun toLayerIdentifier(): String {
        return "SurfaceViewBackground"
    }
}

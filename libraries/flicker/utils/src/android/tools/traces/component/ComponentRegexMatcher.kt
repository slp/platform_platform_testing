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

/** ComponentMatcher based on a regular expression for a name */
data class ComponentRegexMatcher(
    private val regex: Regex
) : IComponentMatcher {

    private val identifierDescription: String =
        "Regular expression: $regex"

    /** {@inheritDoc} */
    override fun windowMatchesAnyOf(windows: Collection<WindowContainer>): Boolean =
        windows.any { regex.matches(it.name) }

    /** {@inheritDoc} */
    override fun activityMatchesAnyOf(activities: Collection<Activity>): Boolean =
        activities.any { regex.matches(it.name) }

    /** {@inheritDoc} */
    override fun layerMatchesAnyOf(layers: Collection<Layer>): Boolean =
        layers.any { regex.matches(it.name) }

    /** {@inheritDoc} */
    override fun check(
        layers: Collection<Layer>,
        condition: (Collection<Layer>) -> Boolean
    ): Boolean = condition(layers.filter { layerMatchesAnyOf(it) })

    /** {@inheritDoc} */
    override fun toActivityIdentifier(): String = identifierDescription

    /** {@inheritDoc} */
    override fun toWindowIdentifier(): String = identifierDescription

    /** {@inheritDoc} */
    override fun toLayerIdentifier(): String = identifierDescription

    companion object {
        val FOLD_OVERLAY_MATCHER =
            ComponentRegexMatcher(
                regex = "^fold-animation-overlay.*".toRegex()
            )
    }
}

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

package android.tools.flicker.config.desktopmode

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertors.ComponentTemplate
import android.tools.flicker.config.ScenarioId
import android.tools.helpers.SYSTEMUI_PACKAGE
import android.tools.traces.component.ComponentNameMatcher
import android.tools.traces.component.FullComponentIdMatcher

object Components {
    val DESKTOP_MODE_CAPTION =
        ComponentTemplate("APP_HEADER") { ComponentNameMatcher(SYSTEMUI_PACKAGE, "caption_handle") }

    val DESKTOP_MODE_APP =
        ComponentTemplate("DESKTOP_MODE_APP") { scenarioInstance: ScenarioInstance ->
            val associatedTransition =
                scenarioInstance.associatedTransition
                    ?: error("Can only extract DESKTOP_MODE_APP from scenario with transition")

            if (isSupported(scenarioInstance.type)) {
                val change = associatedTransition.changes.last()
                FullComponentIdMatcher(change.windowId, change.layerId)
            } else {
                error("Unsupported transition type")
            }
        }

    private fun isSupported(type: ScenarioId): Boolean {
        return when (type) {
            ScenarioId("END_DRAG_TO_DESKTOP") -> true
            ScenarioId("CLOSE_APP") -> true
            ScenarioId("CLOSE_LAST_APP") -> true
            ScenarioId("CORNER_RESIZE") -> true
            else -> false
        }
    }
}

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
import android.tools.traces.component.IComponentMatcher
import android.tools.traces.wm.Transition
import android.tools.traces.wm.TransitionType

object Components {
    val DESKTOP_MODE_CAPTION =
        ComponentTemplate("APP_HEADER") { ComponentNameMatcher(SYSTEMUI_PACKAGE, "caption_handle") }

    val DESKTOP_MODE_APP =
        ComponentTemplate("DESKTOP_MODE_APP") { scenarioInstance: ScenarioInstance ->
            val associatedTransition =
                scenarioInstance.associatedTransition
                    ?: error("Can only extract DESKTOP_MODE_APP from scenario with transition")

            getDesktopAppForScenario(scenarioInstance.type, associatedTransition)
        }

    val SIMPLE_APP =
        ComponentTemplate("SIMPLE_APP") {
            ComponentNameMatcher(
                "com.android.server.wm.flicker.testapp",
                "com.android.server.wm.flicker.testapp.SimpleActivity",
            )
        }

    val NON_RESIZABLE_APP =
        ComponentTemplate("NON_RESIZABLE_APP") {
            ComponentNameMatcher(
                "com.android.server.wm.flicker.testapp",
                "com.android.server.wm.flicker.testapp.NonResizeableActivity",
            )
        }

    val DESKTOP_WALLPAPER =
        ComponentTemplate("DesktopWallpaper") {
            ComponentNameMatcher(
                SYSTEMUI_PACKAGE,
                "com.android.wm.shell.desktopmode.DesktopWallpaperActivity",
            )
        }

    private fun getDesktopAppForScenario(
        type: ScenarioId,
        associatedTransition: Transition,
    ): IComponentMatcher {
        return when (type) {
            ScenarioId("END_DRAG_TO_DESKTOP") -> {
                val change =
                    associatedTransition.changes.first { it.transitMode == TransitionType.CHANGE }
                FullComponentIdMatcher(change.windowId, change.layerId)
            }
            ScenarioId("CLOSE_APP"),
            ScenarioId("CLOSE_LAST_APP") -> {
                val change =
                    associatedTransition.changes.first { it.transitMode == TransitionType.CLOSE }
                FullComponentIdMatcher(change.windowId, change.layerId)
            }
            ScenarioId("OPEN_UNLIMITED_APPS"),
            ScenarioId("CASCADE_APP") -> {
                val change =
                    associatedTransition.changes.first { it.transitMode == TransitionType.OPEN }
                FullComponentIdMatcher(change.windowId, change.layerId)
            }
            ScenarioId("MINIMIZE_APP"),
            ScenarioId("MINIMIZE_LAST_APP") -> {
                val change =
                    associatedTransition.changes.first { it.transitMode == TransitionType.TO_BACK }
                FullComponentIdMatcher(change.windowId, change.layerId)
            }
            ScenarioId("CORNER_RESIZE"),
            ScenarioId("CORNER_RESIZE_TO_MINIMUM_SIZE"),
            ScenarioId("CORNER_RESIZE_TO_MAXIMUM_SIZE"),
            ScenarioId("EDGE_RESIZE"),
            ScenarioId("SNAP_RESIZE_LEFT_WITH_DRAG"),
            ScenarioId("SNAP_RESIZE_RIGHT_WITH_DRAG"),
            ScenarioId("SNAP_RESIZE_LEFT_WITH_BUTTON"),
            ScenarioId("SNAP_RESIZE_RIGHT_WITH_BUTTON"),
            ScenarioId("MAXIMIZE_APP"),
            ScenarioId("MAXIMIZE_APP_NON_RESIZABLE"),
            ScenarioId("MINIMIZE_AUTO_PIP_APP") -> {
                val change = associatedTransition.changes.first()
                FullComponentIdMatcher(change.windowId, change.layerId)
            }
            ScenarioId("BRING_APPS_TO_FRONT") -> {
                val change =
                    associatedTransition.changes.first { it.transitMode == TransitionType.TO_FRONT }
                FullComponentIdMatcher(change.windowId, change.layerId)
            }
            else -> error("Unsupported transition type")
        }
    }
}

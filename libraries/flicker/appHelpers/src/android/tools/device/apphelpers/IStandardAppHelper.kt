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

package android.tools.device.apphelpers

import android.content.Intent
import android.tools.traces.ConditionsFactory
import android.tools.traces.component.IComponentMatcher
import android.tools.traces.component.IComponentNameMatcher
import android.tools.traces.parsers.WindowManagerStateHelper

interface IStandardAppHelper : IComponentNameMatcher {
    fun open()

    fun exit()

    /** Exits the activity and wait for activity destroyed */
    fun exit(wmHelper: WindowManagerStateHelper)

    /**
     * Launches the app through an intent instead of interacting with the launcher.
     *
     * Uses UiAutomation to detect when the app is open
     */
    fun launchViaIntent(
        expectedPackageName: String = "",
        action: String? = null,
        stringExtras: Map<String, String> = mapOf()
    )

    /**
     * Launches the app through an intent instead of interacting with the launcher and waits until
     * the app window is visible
     */
    fun launchViaIntent(
        wmHelper: WindowManagerStateHelper,
        launchedAppComponentMatcherOverride: IComponentMatcher? = null,
        action: String? = null,
        stringExtras: Map<String, String> = mapOf(),
        waitConditionsBuilder: WindowManagerStateHelper.StateSyncBuilder =
            wmHelper
                .StateSyncBuilder()
                .add(ConditionsFactory.isWMStateComplete())
                .withAppTransitionIdle()
    )

    /**
     * Launches the app through an intent instead of interacting with the launcher and waits until
     * the app window is visible
     */
    fun launchViaIntent(
        wmHelper: WindowManagerStateHelper,
        intent: Intent,
        launchedAppComponentMatcherOverride: IComponentMatcher? = null,
        waitConditionsBuilder: WindowManagerStateHelper.StateSyncBuilder =
            wmHelper
                .StateSyncBuilder()
                .add(ConditionsFactory.isWMStateComplete())
                .withAppTransitionIdle()
    )

    fun isAvailable(): Boolean
}

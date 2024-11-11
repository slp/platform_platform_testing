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

package platform.test.screenshot

import android.os.UserHandle
import android.view.Display
import android.view.WindowManagerGlobal
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule to reset forced display and density, as set by the [DeviceEmulationRule].
 *
 * NOTE: Use this only as a `@ClassRule` to cleanup after the test class finishes, and only in
 * scenarios where the vast majority of tests cases does not depend on the [DeviceEmulationRule]:
 * Resetting the display settings comes at a runtime cost for tests, which is unnecessary if the
 * next test is simply forcing setting the display setting again.
 *
 * ```
 * @RunWith(AndroidJUnit4::class)
 * class MyTest {
 *   companion object {
 *     @JvmField @ClassRule val cleanupRule: ResetDeviceEmulationRule = ResetDeviceEmulationRule()
 *   }
 *
 *   @get:Rule val emulationRule = DeviceEmulationRule(/*...*/)
 *
 *   // ...
 * }
 * ```
 */
class ResetDeviceEmulationRule : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base?.evaluate()
                } finally {
                    clearForcedDisplaySettings()

                    // Reset the DeviceEmulationRule's in-memory cache about display setting have
                    // been set.
                    DeviceEmulationRule.prevDensity = -1
                    DeviceEmulationRule.prevWidth = -1
                    DeviceEmulationRule.prevHeight = -1
                }
            }
        }

    private fun clearForcedDisplaySettings() {
        val wm =
            WindowManagerGlobal.getWindowManagerService()
                ?: error("Unable to acquire WindowManager")
        wm.clearForcedDisplaySize(Display.DEFAULT_DISPLAY)
        wm.clearForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, UserHandle.myUserId())
    }
}

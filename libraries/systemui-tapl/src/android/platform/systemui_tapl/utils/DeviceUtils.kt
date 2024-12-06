/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.platform.systemui_tapl.utils

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import java.time.Duration

const val SYSUI_PACKAGE = "com.android.systemui"
const val SETTINGS_PACKAGE = "com.android.settings"
private const val LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher"
private const val ANDROID_PACKAGE = "android"

object DeviceUtils {
    @JvmField val SHORT_WAIT: Duration = Duration.ofMillis(1_500)
    @JvmField val LONG_WAIT: Duration = Duration.ofMillis(10_000)

    /** Returns a [BySelector] of a resource in sysui package. */
    @JvmStatic
    fun sysuiResSelector(resourceId: String): BySelector =
        By.pkg(SYSUI_PACKAGE).res(SYSUI_PACKAGE, resourceId)

    /** Returns a [BySelector] of a resource in settings package. */
    @JvmStatic
    fun settingsResSelector(resourceId: String): BySelector =
        By.pkg(SETTINGS_PACKAGE).res(SETTINGS_PACKAGE, resourceId)

    /** Returns a [BySelector] of a resource in launcher package. */
    @JvmStatic
    fun launcherResSelector(resourceId: String): BySelector =
        By.pkg(LAUNCHER_PACKAGE).res(LAUNCHER_PACKAGE, resourceId)

    /** Returns a [BySelector] of a resource with the given content description in sysui package. */
    @JvmStatic
    fun sysuiDescSelector(contentDescription: String): BySelector =
        By.pkg(SYSUI_PACKAGE).desc(contentDescription)

    /**
     * Returns a [BySelector] of a resource with the given content description in launcher package.
     */
    @JvmStatic
    fun launcherDescSelector(contentDescription: String): BySelector =
        By.pkg(LAUNCHER_PACKAGE).desc(contentDescription)

    /** Returns a [BySelector] of a resource in android package. */
    @JvmStatic
    fun androidResSelector(resourceId: String): BySelector = By.res(ANDROID_PACKAGE, resourceId)
}

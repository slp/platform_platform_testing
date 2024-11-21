/*
 * Copyright (C) 2023 The Android Open Source Project
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

@file:JvmName("CommonConstants")

package android.tools.testutils

import android.tools.traces.component.ComponentNameMatcher

object TestComponents {
    val CHROME = ComponentNameMatcher("com.android.chrome", "com.google.android.apps.chrome.Main")

    val CHROME_FIRST_RUN =
        ComponentNameMatcher(
            "com.android.chrome",
            "org.chromium.chrome.browser.firstrun.FirstRunActivity",
        )

    val CHROME_SPLASH_SCREEN = ComponentNameMatcher("", "Splash Screen com.android.chrome")

    val DOCKER_STACK_DIVIDER = ComponentNameMatcher("", "DockedStackDivider")

    val IMAGINARY = ComponentNameMatcher("", "ImaginaryWindow")

    val IME_ACTIVITY =
        ComponentNameMatcher(
            "com.android.server.wm.flicker.testapp",
            "com.android.server.wm.flicker.testapp.ImeActivity",
        )

    val LAUNCHER =
        ComponentNameMatcher(
            "com.google.android.apps.nexuslauncher",
            "com.google.android.apps.nexuslauncher.NexusLauncherActivity",
        )

    val PIP_OVERLAY = ComponentNameMatcher("", "pip-dismiss-overlay")

    val SIMPLE_APP =
        ComponentNameMatcher(
            "com.android.server.wm.flicker.testapp",
            "com.android.server.wm.flicker.testapp.SimpleActivity",
        )

    val NON_RESIZEABLE_APP =
        ComponentNameMatcher(
            "com.android.server.wm.flicker.testapp",
            "com.android.server.wm.flicker.testapp.NonResizeableActivity",
        )

    private const val SHELL_PKG_NAME = "com.android.wm.shell.flicker.testapp"

    val SHELL_SPLIT_SCREEN_PRIMARY =
        ComponentNameMatcher(SHELL_PKG_NAME, "$SHELL_PKG_NAME.SplitScreenActivity")

    val SHELL_SPLIT_SCREEN_SECONDARY =
        ComponentNameMatcher(SHELL_PKG_NAME, "$SHELL_PKG_NAME.SplitScreenSecondaryActivity")

    val FIXED_APP = ComponentNameMatcher(SHELL_PKG_NAME, "$SHELL_PKG_NAME.FixedActivity")

    val PIP_APP = ComponentNameMatcher(SHELL_PKG_NAME, "$SHELL_PKG_NAME.PipActivity")

    val SCREEN_DECOR_OVERLAY = ComponentNameMatcher("", "ScreenDecorOverlay")

    val WALLPAPER =
        ComponentNameMatcher(
            "",
            "com.breel.wallpapers18.soundviz.wallpaper.variations.SoundVizWallpaperV2",
        )
}

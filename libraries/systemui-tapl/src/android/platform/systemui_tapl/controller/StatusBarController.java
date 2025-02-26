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

package android.platform.systemui_tapl.controller;

import static android.platform.uiautomator_helpers.DeviceHelpers.getUiDevice;
import static android.platform.uiautomator_helpers.DeviceHelpers.shell;

/** Controller for manipulating status bar state. */
public class StatusBarController {
    private static final String SHOW_BATTERY_PERCENT_SETTING = "status_bar_show_battery_percent";

    private StatusBarController() {}

    /** Returns an instance of StatusBarController. */
    public static StatusBarController get() {
        return new StatusBarController();
    }

    /** If the user wants to see the batter percentage on the status bar. */
    private static boolean shouldShowBatteryPercentage() {
        return "1"
                .equals(
                        shell(getUiDevice(), "settings get system " + SHOW_BATTERY_PERCENT_SETTING)
                                .trim());
    }

    private static void setBatteryPercentageVisibleInternal(boolean visible) {
        shell(
                getUiDevice(),
                "settings put system " + SHOW_BATTERY_PERCENT_SETTING + " " + (visible ? 1 : 0));
    }

    /**
     * Shows or hides the battery percentage indicator.
     *
     * @param visible show?
     * @return previous visibility.
     */
    public boolean setBatteryPercentageVisible(boolean visible) {
        boolean wasPercentageVisible = shouldShowBatteryPercentage();
        setBatteryPercentageVisibleInternal(visible);
        return wasPercentageVisible;
    }
}

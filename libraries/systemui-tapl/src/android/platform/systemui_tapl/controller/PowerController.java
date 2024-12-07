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

import static android.platform.helpers.CommonUtils.executeShellCommand;
import static android.platform.uiautomator_helpers.DeviceHelpers.getContext;
import static android.view.KeyEvent.KEYCODE_POWER;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/** Controller for manipulating power button behavior and actions. */
public class PowerController {
    private static final String TAG = "PowerController";
    private static final String POWER_COMMAND =
            String.format("input keyevent --longpress  %s", KEYCODE_POWER);
    private static final String POWER_BUTTON_LONG_PRESS_BEHAVIOR = "power_button_long_press";

    /** Returns an instance of PowerController. */
    public static PowerController get() {
        return new PowerController();
    }

    private PowerController() {}

    /** Mimics long-pressing the power button. */
    public void longPressPowerButton() {
        executeShellCommand(POWER_COMMAND);
    }

    /**
     * Sets the behavior of the long press of the power button. See config_longPressOnPowerBehavior.
     *
     * @param behavior new behavior
     * @return old behavior
     */
    public int setLongPressOnPowerBehavior(int behavior) {
        final Context context = getContext();
        int oldLongPressPowerKeyBehavior;
        try {
            oldLongPressPowerKeyBehavior =
                    Settings.Global.getInt(
                            context.getContentResolver(), POWER_BUTTON_LONG_PRESS_BEHAVIOR);
        } catch (Settings.SettingNotFoundException e) {
            Log.w(TAG, "Global settings POWER_BUTTON_LONG_PRESS do not exist.");
            oldLongPressPowerKeyBehavior = 0;
        }
        assertThat(
                        Settings.Global.putInt(
                                context.getContentResolver(),
                                POWER_BUTTON_LONG_PRESS_BEHAVIOR,
                                behavior))
                .isTrue();

        return oldLongPressPowerKeyBehavior;
    }
}

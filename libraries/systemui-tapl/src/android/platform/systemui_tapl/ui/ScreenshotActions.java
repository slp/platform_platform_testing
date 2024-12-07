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

package android.platform.systemui_tapl.ui;

import static android.platform.systemui_tapl.utils.DeviceUtils.sysuiDescSelector;

import android.platform.test.scenario.tapl_common.TaplUiDevice;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.BySelector;

/**
 * System UI test automation object representing actions panel that appears after taking a
 * screenshot.
 */
public class ScreenshotActions {
    private static final BySelector SHARE_BUTTON = sysuiDescSelector("Share screenshot");

    ScreenshotActions() {}

    /** Opens the share sheet by clicking the "Share" button. */
    @NonNull
    public ShareSheet openSharesheet() {
        TaplUiDevice.waitForObject(SHARE_BUTTON, "Share button").click();

        return new ShareSheet();
    }
}

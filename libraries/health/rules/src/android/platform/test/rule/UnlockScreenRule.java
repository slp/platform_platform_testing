/*
 * Copyright (C) 2019 The Android Open Source Project
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
package android.platform.test.rule;

import static android.platform.uiautomator_helpers.DeviceHelpers.assertInvisible;

import static com.android.systemui.Flags.keyguardBottomAreaRefactor;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;

import org.junit.runner.Description;

import java.time.Duration;

/** This rule will unlock phone screen before a test case. */
public class UnlockScreenRule extends TestWatcher {

    protected static final BySelector KEYGUARD_BOTTOM_AREA_VIEW =
            By.res("com.android.systemui", "keyguard_bottom_area");

    protected static final BySelector KEYGUARD_ROOT_VIEW =
            By.res("com.android.systemui", "keyguard_root_view");

    @Override
    protected void starting(Description description) {
        unlockScreen(getUiDevice());
    }

    /**
     * Unlocks the screen on the given device and asserts that the lock screen has disappeared.
     *
     * @param uiDevice the device to unlock the screen for.
     */
    public static void unlockScreen(@NonNull UiDevice uiDevice) {
        try {
            // Turn on the screen if necessary.
            if (!uiDevice.isScreenOn()) {
                uiDevice.wakeUp();
            }

            BySelector screenLock;
            if (keyguardBottomAreaRefactor()) {
                screenLock = KEYGUARD_ROOT_VIEW;
            } else {
                screenLock = KEYGUARD_BOTTOM_AREA_VIEW;
            }

            if (uiDevice.hasObject(screenLock)) {
                uiDevice.pressMenu();
                uiDevice.waitForIdle();
                assertInvisible(screenLock, /* timeout= */ Duration.ofSeconds(20));
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Could not unlock device.", e);
        }
    }
}

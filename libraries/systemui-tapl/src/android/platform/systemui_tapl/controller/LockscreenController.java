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

import static android.content.Context.KEYGUARD_SERVICE;
import static android.platform.helpers.LockscreenUtils.LockscreenType.NONE;
import static android.platform.helpers.LockscreenUtils.LockscreenType.PASSWORD;
import static android.platform.helpers.LockscreenUtils.LockscreenType.PATTERN;
import static android.platform.helpers.LockscreenUtils.LockscreenType.PIN;
import static android.platform.helpers.LockscreenUtils.LockscreenType.SWIPE;
import static android.platform.test.util.HealthTestingUtils.waitForCondition;
import static android.platform.uiautomator_helpers.DeviceHelpers.getContext;
import static android.platform.uiautomator_helpers.DeviceHelpers.getUiDevice;
import static android.platform.uiautomator_helpers.DeviceHelpers.isScreenOnSettled;
import static android.platform.uiautomator_helpers.WaitUtils.ensureThat;

import static com.google.common.truth.Truth.assertThat;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.platform.helpers.LockscreenUtils;
import android.provider.Settings;

import org.junit.Assume;

import java.io.IOException;

/** Controller for manipulating lockscreen states. */
public class LockscreenController {
    private static final int SLEEP_INTERVAL_MS = 500;

    private static final String FACE_WAKE_FAKE_COMMAND =
            "am broadcast -a "
                    + "com.android.systemui.latency.ACTION_FACE_WAKE"
                    + " --user 0"; // Making sure broadcast receiver will receive the action
    // if current user is different than System.
    private static final String FINGERPRINT_WAKE_FAKE_COMMAND =
            "am broadcast -a "
                    + "com.android.systemui.latency.ACTION_FINGERPRINT_WAKE"
                    + " --user 0"; // Making sure broadcast receiver will receive the action

    // if current user is different than System.

    /** Returns an instance of LockscreenController. */
    public static LockscreenController get() {
        return new LockscreenController();
    }

    private LockscreenController() {}

    /** Enables unlocking via swipe */
    public void setUnlockSwipe() {
        LockscreenUtils.setLockscreen(
                /* lockscreenType= */ SWIPE, /* lockscreenCode= */ "", /* expectedResult= */ false);
    }

    /** Enables no-lockscreen mode */
    public void setNoLockScreenMode() {
        LockscreenUtils.setLockscreen(
                /* lockscreenType= */ NONE, /* lockscreenCode= */ "", /* expectedResult= */ false);
    }

    /** Enables pin unlock */
    public void setLockscreenPin(String pin) {
        LockscreenUtils.setLockscreen(
                /* lockscreenType= */ PIN, /* lockscreenCode= */ pin, /* expectedResult= */ true);
    }

    /** Enables password unlock */
    public void setLockscreenPassword(String password) {
        LockscreenUtils.setLockscreen(
                /* lockscreenType= */ PASSWORD,
                /* lockscreenCode= */ password,
                /* expectedResult= */ true);
    }

    /** Enables pattern unlock */
    public void setLockscreenPattern(String pattern) {
        LockscreenUtils.setLockscreen(
                /* lockscreenType= */ PATTERN,
                /* lockscreenCode= */ pattern,
                /* expectedResult= */ true);
    }

    /**
     * Enables or disables always-on display.
     *
     * @param enableAod Enable AOD?
     * @return whether AOD was enabled.
     */
    public boolean setAodEnabled(boolean enableAod) {
        final ContentResolver contentResolver = getContext().getContentResolver();
        final boolean aodWasEnabled =
                Settings.Secure.getInt(contentResolver, Settings.Secure.DOZE_ALWAYS_ON, 0) == 1;

        if (enableAod != aodWasEnabled) {
            assertThat(
                            Settings.Secure.putInt(
                                    contentResolver,
                                    Settings.Secure.DOZE_ALWAYS_ON,
                                    enableAod ? 1 : 0))
                    .isTrue();
        }

        return aodWasEnabled;
    }

    /** Turns screen off by going to sleep. */
    public void turnScreenOff() {
        try {
            getUiDevice().sleep();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        SystemClock.sleep(SLEEP_INTERVAL_MS * 4);
        waitForCondition(() -> "Screen didn't turn off", () -> !getUiDevice().isScreenOn());
    }

    /** Turns screen on by waking up from sleep. */
    public void turnScreenOn() {
        Trace.beginSection("LockscreenController#turnScreenOn");
        try {
            try {
                getUiDevice().wakeUp();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            SystemClock.sleep(SLEEP_INTERVAL_MS * 4);
            ensureThat("Screen is on", () -> isScreenOnSettled(getUiDevice()));
        } finally {
            Trace.endSection();
        }
    }

    /** Goes to the Locked screen page */
    public void lockScreen() {
        LockscreenUtils.goToLockScreen();
    }

    /**
     * Clears the lock credentials.
     *
     * @param currentLockscreenCode old code which is currently set.
     */
    public void clearLockCredentials(String currentLockscreenCode) {
        LockscreenUtils.resetLockscreen(currentLockscreenCode);
    }

    /** Fake face unlock. */
    public void fakeFaceUnlock() {
        try {
            getUiDevice().executeShellCommand(FACE_WAKE_FAKE_COMMAND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Fake fingerprint unlock. */
    public void fakeFingerprintUnlock() {
        try {
            getUiDevice().executeShellCommand(FINGERPRINT_WAKE_FAKE_COMMAND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyguardManager getKeyguardManager() {
        return (KeyguardManager) getContext().getSystemService(KEYGUARD_SERVICE);
    }

    /** Checks whether the device supports AlwaysOnDisplay feature. */
    public static void skipIfDeviceDoesNotSupportAod() {
        AmbientDisplayConfiguration config = new AmbientDisplayConfiguration(getContext());
        Assume.assumeTrue(
                "Device dose not support AOD, skipped test.",
                config.alwaysOnAvailable() || getUiDevice().getProductName().startsWith("cf_x86"));
    }

    /**
     * Returns whether the device is currently locked and requires a PIN, pattern or password to
     * unlock. see [KeyguardManager.isDeviceLocked].
     */
    public boolean isDeviceLocked() {
        return getKeyguardManager().isDeviceLocked();
    }

    /**
     * Returns whether the device is currently secured by a PIN, pattern or password. see
     * [KeyguardManager.isDeviceSecure].
     */
    public boolean isDeviceSecure() {
        return getKeyguardManager().isDeviceSecure();
    }

    /**
     * Returns whether the keyguard is currently locked. See [KeyguardManager.isKeyguardLocked].
     * Experimental.
     */
    public boolean isKeyguardLocked() {
        return getKeyguardManager().isKeyguardLocked();
    }
}

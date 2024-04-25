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

package com.android.sts.common;

import static java.lang.String.format;

import android.app.KeyguardManager;
import android.content.Context;

import com.android.compatibility.common.util.ShellUtils;

/** Util to manage lock settings */
public class LockSettingsUtil {
    private Context mContext;
    public static final String DEFAULT_LOCKSCREEN_CODE = "1234";

    /**
     * Create an instance of LockSettingsUtil.
     *
     * @param context the test context {@link Context}.
     * @throws IllegalArgumentException when {@code context} is null.
     */
    public LockSettingsUtil(Context context) throws IllegalArgumentException {
        // Context should not be null
        if (context == null) {
            throw new IllegalArgumentException("Context should not be null");
        }
        mContext = context;
    }

    /** Different options for setting the lock screen */
    private enum LockScreenType {
        PIN,
        PASSWORD,
        PATTERN,
        SWIPE
    }

    /**
     * Set the lock screen using a default pin and remove it upon closing.
     *
     * @return AutoCloseable that clears the lock upon closing
     */
    public AutoCloseable withLockScreen()
            throws IllegalStateException, IllegalArgumentException, InterruptedException {
        return this.withPin(DEFAULT_LOCKSCREEN_CODE);
    }

    /**
     * Set the lockscreen using a pin and remove it upon closing.
     *
     * @param pin the pin that needs to be set
     * @return AutoCloseable that clears the pin upon closing
     */
    public AutoCloseable withPin(String pin)
            throws IllegalStateException, IllegalArgumentException, InterruptedException {
        return this.withLockScreen(LockScreenType.PIN, pin);
    }

    /**
     * Set the lockscreen using a pattern and remove it upon closing.
     *
     * @param pattern the pattern that needs to be set
     * @return AutoCloseable that clears the pattern upon closing
     */
    public AutoCloseable withPattern(String pattern)
            throws IllegalStateException, IllegalArgumentException, InterruptedException {
        return this.withLockScreen(LockScreenType.PATTERN, pattern);
    }

    /**
     * Set the lockscreen using a password and remove it upon closing.
     *
     * @param password the password that needs to be set
     * @return AutoCloseable that clears the password upon closing
     */
    public AutoCloseable withPassword(String password)
            throws IllegalStateException, IllegalArgumentException, InterruptedException {
        return this.withLockScreen(LockScreenType.PASSWORD, password);
    }

    /**
     * Set the swipe lockscreen and remove it upon closing.
     *
     * @return AutoCloseable that clears the swipe lock upon closing
     */
    public AutoCloseable withSwipe()
            throws IllegalStateException, IllegalArgumentException, InterruptedException {
        return this.withLockScreen(LockScreenType.SWIPE, null);
    }

    /**
     * Set the lockscreen using either of a pin, pattern, password, or swipe and remove it upon
     * closing.
     *
     * @param instrumentation {@link Instrumentation} instance, obtained from a test running in
     *     instrumentation framework.
     * @param lockScreenType the lock screen type, it can take one of the values defined in the enum
     *     LockScreenType.
     * @param lockScreenCode the code[PIN or PATTERN or PASSWORD] that needs to be set
     * @return AutoCloseable that clears the lock screen upon closing
     * @throws IllegalArgumentException if the lock screen type is not supported or if lock screen
     *     code is null when it's not "SWIPE".
     * @throws IllegalStateException if device was already secure before setting the lock or if we
     *     are unable to secure the device after attempting to set the lock.
     * @throws InterruptedException if the polling thread is interrupted
     */
    private AutoCloseable withLockScreen(LockScreenType lockScreenType, String lockScreenCode)
            throws IllegalStateException, IllegalArgumentException, InterruptedException {
        // If the lock screen type is not "SWIPE" then the lock screen code cannot be null
        if (lockScreenType != LockScreenType.SWIPE && lockScreenCode == null) {
            throw new IllegalArgumentException("Lock screen code cannot be null");
        }

        // Check the keyguard status, do not proceed if it's already secure
        KeyguardManager keyguardManager = mContext.getSystemService(KeyguardManager.class);
        if (keyguardManager.isDeviceSecure()) {
            throw new IllegalStateException("Keyguard is already secure");
        }

        // Enable the lock using the adb shell command corresponding to the lock type
        switch (lockScreenType) {
            case PIN:
                ShellUtils.runShellCommand(format("locksettings set-pin %s", lockScreenCode));
                break;
            case PASSWORD:
                ShellUtils.runShellCommand(format("locksettings set-password %s", lockScreenCode));
                break;
            case PATTERN:
                ShellUtils.runShellCommand(format("locksettings set-pattern %s", lockScreenCode));
                break;
            case SWIPE:
                ShellUtils.runShellCommand("locksettings set-disabled false");
                break;
            default:
                throw new IllegalArgumentException(
                        format("Non-supported Lockscreen Type: %s", lockScreenType));
        }

        // If the lock type is "SWIPE", then check if it is set successfully otherwise wait for
        // the lock to be enabled
        if (lockScreenType == LockScreenType.SWIPE) {
            String disabled = ShellUtils.runShellCommand("locksettings get-disabled");
            if (!disabled.trim().equals("false")) {
                throw new IllegalStateException("Failed to enable the swipe lockscreen");
            }
        } else if (!SystemUtil.poll(() -> keyguardManager.isDeviceSecure())) {
            throw new IllegalStateException("Failed to secure the device");
        }

        return new AutoCloseable() {
            @Override
            public void close() {
                // Clear the lock screen code
                if (lockScreenType == LockScreenType.SWIPE) {
                    ShellUtils.runShellCommand("locksettings set-disabled true");
                } else {
                    ShellUtils.runShellCommand("locksettings clear --old %s", lockScreenCode);
                }
            }
        };
    }
}

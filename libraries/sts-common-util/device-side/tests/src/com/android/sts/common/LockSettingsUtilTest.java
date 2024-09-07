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

package com.android.sts.common.util.tests;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.android.compatibility.common.util.ShellUtils.runShellCommand;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.KeyguardManager;
import android.content.Context;

import com.android.sts.common.LockSettingsUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link LockSettingsUtil}. */
public class LockSettingsUtilTest {
    private LockSettingsUtil mLockSettingsUtil;
    private KeyguardManager mKeyguardManager;
    private static final String TEST_CREDENTIALS = "1234";

    @Before
    public void setUp() throws Exception {
        Context context = getInstrumentation().getContext();
        mLockSettingsUtil = new LockSettingsUtil(context);
        mKeyguardManager = context.getSystemService(KeyguardManager.class);
        assertWithMessage("Device already secure")
                .that(mKeyguardManager.isDeviceSecure())
                .isFalse();
    }

    @After
    public void tearDown() throws Exception {
        assertWithMessage("Did not clear the lock screen")
                .that(mKeyguardManager.isDeviceSecure())
                .isFalse();
    }

    @Test
    public void testSetDefaultLockScreen() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withLockScreen()) {
            assertWithMessage("Did not lock the device")
                    .that(mKeyguardManager.isDeviceSecure())
                    .isTrue();
        }
    }

    @Test
    public void testSetPin() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withPin(TEST_CREDENTIALS)) {
            assertWithMessage("Did not lock the device with a pin")
                    .that(mKeyguardManager.isDeviceSecure())
                    .isTrue();
        }
    }

    @Test
    public void testSetPassword() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withPassword(TEST_CREDENTIALS)) {
            assertWithMessage("Did not lock the device with a password")
                    .that(mKeyguardManager.isDeviceSecure())
                    .isTrue();
        }
    }

    @Test
    public void testSetPattern() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withPattern(TEST_CREDENTIALS)) {
            assertWithMessage("Did not lock the device with a pattern")
                    .that(mKeyguardManager.isDeviceSecure())
                    .isTrue();
        }
    }

    @Test
    public void testSetSwipeLock() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withSwipe()) {
            String disabled = runShellCommand("locksettings get-disabled");
            assertWithMessage("Did not lock the device with a swipe screen")
                    .that(disabled.trim())
                    .isEqualTo("false");
        }
    }
}

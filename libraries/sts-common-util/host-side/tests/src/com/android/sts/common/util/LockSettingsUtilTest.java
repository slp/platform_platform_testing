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

package com.android.sts.common.util;

import static com.android.sts.common.LockSettingsUtil.DEFAULT_LOCKSCREEN_CODE;

import static com.google.common.truth.Truth.assertWithMessage;

import com.android.sts.common.LockSettingsUtil;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.util.CommandResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link LockSettingsUtil}. */
@RunWith(DeviceJUnit4ClassRunner.class)
public class LockSettingsUtilTest extends BaseHostJUnit4Test {
    private LockSettingsUtil mLockSettingsUtil;
    private static final String TEST_CREDENTIALS = "123456";
    private ITestDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = getDevice();
        mLockSettingsUtil = new LockSettingsUtil(mDevice);

        // Assert that keyguard is not already secure
        assertLockCredentialsCleared("Device already secure!");
    }

    @Test
    public void testDefaultLockScreenNonRoot() throws Exception {
        assertWithMessage("Must be able to unroot the device")
                .that(mDevice.disableAdbRoot())
                .isTrue();

        testDefaultLockScreen();

        assertWithMessage("Device should not implicitly root to cleanup")
                .that(mDevice.isAdbRoot())
                .isFalse();
    }

    @Test
    public void testSetPinNonRoot() throws Exception {
        assertWithMessage("Must be able to unroot the device")
                .that(mDevice.disableAdbRoot())
                .isTrue();

        testSetPin();

        assertWithMessage("Device should not implicitly root to cleanup")
                .that(mDevice.isAdbRoot())
                .isFalse();
    }

    @Test
    public void testSetPasswordNonRoot() throws Exception {
        assertWithMessage("Must be able to unroot the device")
                .that(mDevice.disableAdbRoot())
                .isTrue();

        testSetPassword();

        assertWithMessage("Device should not implicitly root to cleanup")
                .that(mDevice.isAdbRoot())
                .isFalse();
    }

    @Test
    public void testSetPatternNonRoot() throws Exception {
        assertWithMessage("Must be able to unroot the device")
                .that(mDevice.disableAdbRoot())
                .isTrue();

        testSetPattern();

        assertWithMessage("Device should not implicitly root to cleanup")
                .that(mDevice.isAdbRoot())
                .isFalse();
    }

    @Test
    public void testSetSwipeNonRoot() throws Exception {
        assertWithMessage("Must be able to unroot the device")
                .that(mDevice.disableAdbRoot())
                .isTrue();

        testSetSwipe();

        assertWithMessage("Device should not implicitly root to cleanup")
                .that(mDevice.isAdbRoot())
                .isFalse();
    }

    @Test
    public void testSetDefaultLockScreenRoot() throws Exception {
        assertWithMessage("Must test with rootable device").that(mDevice.enableAdbRoot()).isTrue();

        testDefaultLockScreen();

        assertWithMessage("Device should still be root after cleanup if started with root")
                .that(mDevice.isAdbRoot())
                .isTrue();
    }

    @Test
    public void testSetPinRoot() throws Exception {
        assertWithMessage("Must test with rootable device").that(mDevice.enableAdbRoot()).isTrue();

        testSetPin();

        assertWithMessage("Device should still be root after cleanup if started with root")
                .that(mDevice.isAdbRoot())
                .isTrue();
    }

    @Test
    public void testSetPasswordRoot() throws Exception {
        assertWithMessage("Must test with rootable device").that(mDevice.enableAdbRoot()).isTrue();

        testSetPassword();

        assertWithMessage("Device should still be root after cleanup if started with root")
                .that(mDevice.isAdbRoot())
                .isTrue();
    }

    @Test
    public void testSetPatternRoot() throws Exception {
        assertWithMessage("Must test with rootable device").that(mDevice.enableAdbRoot()).isTrue();

        testSetPattern();

        assertWithMessage("Device should still be root after cleanup if started with root")
                .that(mDevice.isAdbRoot())
                .isTrue();
    }

    @Test
    public void testSetSwipeRoot() throws Exception {
        assertWithMessage("Must test with rootable device").that(mDevice.enableAdbRoot()).isTrue();

        testSetSwipe();

        assertWithMessage("Device should still be root after cleanup if started with root")
                .that(mDevice.isAdbRoot())
                .isTrue();
    }

    private void testDefaultLockScreen() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withLockScreen()) {
            assertLockCredentialsSet(DEFAULT_LOCKSCREEN_CODE, "Did not secure the device");
        }
        assertLockCredentialsCleared("Did not clear the lock after use");
    }

    private void testSetPin() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withPin(TEST_CREDENTIALS)) {
            assertLockCredentialsSet(TEST_CREDENTIALS, "Did not secure the device with a pin");
        }
        assertLockCredentialsCleared("Did not clear the pin after use");
    }

    private void testSetPassword() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withPassword(TEST_CREDENTIALS)) {
            assertLockCredentialsSet(TEST_CREDENTIALS, "Did not secure the device with a password");
        }
        assertLockCredentialsCleared("Did not clear the password after use");
    }

    private void testSetPattern() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withPattern(TEST_CREDENTIALS)) {
            assertLockCredentialsSet(TEST_CREDENTIALS, "Did not secure the device with a pattern");
        }
        assertLockCredentialsCleared("Did not clear the pattern after use");
    }

    private void testSetSwipe() throws Exception {
        try (AutoCloseable withLockScreen = mLockSettingsUtil.withSwipe()) {
            assertKeyguardDisabledStatus("false", "Did not set the swipe lockscreen");
        }
        assertKeyguardDisabledStatus("true", "Did not clear the swipe lockscreen after use");
    }

    private void assertLockCredentialsSet(String credentials, String failMessage)
            throws DeviceNotAvailableException {
        CommandResult verifyResult =
                mDevice.executeShellV2Command("locksettings verify --old " + credentials);
        assertWithMessage(failMessage)
                .that(verifyResult.getStdout())
                .ignoringCase()
                .contains("success");
    }

    private void assertLockCredentialsCleared(String failMessage)
            throws DeviceNotAvailableException {
        CommandResult verifyResult = mDevice.executeShellV2Command("locksettings verify");
        assertWithMessage(failMessage)
                .that(verifyResult.getStdout())
                .ignoringCase()
                .contains("success");
    }

    private void assertKeyguardDisabledStatus(String expectedState, String failMessage)
            throws DeviceNotAvailableException {
        CommandResult lockDisabledState =
                mDevice.executeShellV2Command("locksettings get-disabled");
        assertWithMessage(failMessage)
                .that(expectedState)
                .isEqualTo(lockDisabledState.getStdout().trim());
    }
}

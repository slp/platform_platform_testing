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

package com.android.sts.common;

import static com.google.common.truth.Truth.assertWithMessage;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.targetprep.TargetSetupError;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link LockSettingsUtil}. */
@RunWith(DeviceJUnit4ClassRunner.class)
public class LockSettingsUtilDeviceTestWrapper extends BaseHostJUnit4Test {
    private ITestDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = getDevice();
    }

    @Test
    public void testLockSettingsUtilNonRoot() throws Exception {
        assertWithMessage("Must be able to unroot the device")
                .that(mDevice.disableAdbRoot())
                .isTrue();

        installAndRunDeviceTests();

        assertWithMessage("Device should not implicitly root to cleanup")
                .that(mDevice.isAdbRoot())
                .isFalse();
    }

    @Test
    public void testLockSettingsUtilRoot() throws Exception {
        assertWithMessage("Must test with rootable device").that(mDevice.enableAdbRoot()).isTrue();

        installAndRunDeviceTests();

        assertWithMessage("Device should still be root after cleanup if started with root")
                .that(mDevice.isAdbRoot())
                .isTrue();
    }

    private void installAndRunDeviceTests() throws TargetSetupError, DeviceNotAvailableException {
        final String testPkg = "com.android.sts.common.util.tests";
        installPackage("StsCommonUtilDeviceTests.apk");
        runDeviceTests(testPkg, testPkg + ".LockSettingsUtilTest");
    }
}

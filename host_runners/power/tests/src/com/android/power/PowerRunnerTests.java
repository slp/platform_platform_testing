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

package com.android.power;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.invoker.IInvocationContext;
import com.android.tradefed.invoker.InvocationContext;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.result.ITestInvocationListener;

import com.android.power.PowerRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PowerRunnerTests {
    private ITestDevice mDevice;
    private IDevice mIDevice;
    private TestInformation mTestInformation;
    private ITestInvocationListener mITestInvocationListener;
    private PowerRunner mPowerRunner;

    @Before
    public void setUp()
            throws DeviceNotAvailableException,
                    IOException,
                    AdbCommandRejectedException,
                    SyncException,
                    TimeoutException {
        mDevice = Mockito.mock(ITestDevice.class);
        mIDevice = Mockito.mock(IDevice.class);
        mITestInvocationListener = Mockito.mock(ITestInvocationListener.class);

        IInvocationContext context = new InvocationContext();
        context.addAllocatedDevice("device", mDevice);
        mTestInformation = TestInformation.newBuilder().setInvocationContext(context).build();
        Mockito.when(mDevice.getIDevice()).thenReturn(mIDevice);
        Mockito.when(mDevice.waitForDeviceNotAvailable(Mockito.anyLong())).thenReturn(true);
        Mockito.when(mDevice.waitForDeviceAvailable(Mockito.anyLong())).thenReturn(true);
        Mockito.when(mDevice.doesFileExist(Mockito.anyString())).thenReturn(true);
        Mockito.when(mDevice.executeAdbCommand(Mockito.anyString())).thenReturn("Success");
        Mockito.when(mDevice.executeShellCommand(Mockito.anyString())).thenReturn("Success");

        Mockito.when(mIDevice.getMountPoint(Mockito.anyString())).thenReturn("mockedFilePath");
        Mockito.when(mIDevice.getSerialNumber()).thenReturn("mockedSerialNumber");

        Mockito.when(mDevice.pullDir(Mockito.anyString(), Mockito.any(File.class)))
                .thenAnswer(
                        input -> {
                            File destDir = input.getArgument(1);
                            File file1 =
                                    new File(
                                            destDir.getAbsoluteFile()
                                                    + File.separator
                                                    + "filename1.proto");

                            file1.createNewFile();
                            return true;
                        });

        mPowerRunner =
                spy(
                        new PowerRunner() {
                            @Override
                            public ITestDevice getDevice() {
                                return mDevice;
                            }

                            protected Map<String, String> getAllInstrumentationArgs() {
                                HashMap<String, String> map = new HashMap<>();
                                map.put("k1", "v1");
                                map.put("k2", "v2");
                                return map;
                            }

                            @Override
                            public String getPackageName() {
                                return "testPackageName";
                            }

                            @Override
                            public String getRunnerName() {
                                return "testRunnerName";
                            }

                            @Override
                            public String getClassName() {
                                return "testClassName";
                            }
                        });
    }

    /** Positive workflow test: When device is connected and test run is successful */
    @Test
    public void testRunWhenDeviceIsOnline() throws DeviceNotAvailableException, IOException {

        mPowerRunner.run(mTestInformation, mITestInvocationListener);

        Mockito.verify(mDevice, Mockito.times(3)).getMountPoint(Mockito.anyString());
        Mockito.verify(mDevice, Mockito.times(4)).executeShellCommand(Mockito.anyString());
        Mockito.verify(mDevice, Mockito.times(3)).doesFileExist(Mockito.anyString());
        Mockito.verify(mIDevice, Mockito.times(4)).getSerialNumber();
        Mockito.verify(mDevice, Mockito.times(1))
                .pullDir(Mockito.anyString(), Mockito.any(File.class));
        Mockito.verify(mPowerRunner, Mockito.times(1)).setUp();
        Mockito.verify(mPowerRunner, Mockito.times(1)).waitForDeviceToBeDisconnected();
        Mockito.verify(mPowerRunner, Mockito.times(1)).waitForDeviceToBeConnected();
        Mockito.verify(mPowerRunner, Mockito.times(1)).parseInstrumentationResults();
        Mockito.verify(mPowerRunner, Mockito.times(1))
                .parseProtoFile(Mockito.anyString(), Mockito.any(File.class));
    }

    /** Negative test: When device fails to disconnect after the test run is triggered */
    @Test
    public void testRunWhenDeviceIsNotDisconnectedAfterTestStart()
            throws DeviceNotAvailableException, IOException {
        Mockito.when(mIDevice.isOnline()).thenReturn(true);
        try {
            mPowerRunner.run(mTestInformation, mITestInvocationListener);
            fail();
        } catch (RuntimeException e) {
            Assert.assertTrue(
                    e.getMessage()
                            .contains(" not disconnected from host after waiting for 120000"));
        }
    }

    /** Negative test: When device fails to connect back to host after the test run is completed */
    @Test
    public void testRunWhenDeviceIsNotConnectedBackAfterTestComplete()
            throws DeviceNotAvailableException, IOException {
        Mockito.when(mIDevice.isOnline()).thenReturn(false);
        Mockito.when(mIDevice.isOffline()).thenReturn(true);
        try {
            mPowerRunner.run(mTestInformation, mITestInvocationListener);
            fail();
        } catch (RuntimeException e) {
            Assert.assertTrue(
                    e.getMessage().contains("not connected back to host after waiting for"));
        }
    }

    /** Negative workflow test: When proto file is not available on the device after the test run */
    @Test
    public void testParseInstrumentationResultsWhenNoProtoFileIsPresent()
            throws DeviceNotAvailableException, IOException {
        // Just return true and don't create any mock proto file so that there will not be any file
        Mockito.when(mDevice.pullDir(Mockito.anyString(), Mockito.any(File.class)))
                .thenReturn(true);

        try {
            mPowerRunner.run(mTestInformation, mITestInvocationListener);
            fail();
        } catch (RuntimeException e) {
            Assert.assertTrue(
                    e.getMessage().contains("Instrumentation results proto file not found under"));
        }
    }
}

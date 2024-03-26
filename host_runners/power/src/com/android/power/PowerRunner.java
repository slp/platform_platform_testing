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

import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.runner.utils.InstrumentationResultProtoParser;
import com.android.runner.utils.NohupCommandHelper;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.ddmlib.TestRunToTestInvocationForwarder;
import com.android.tradefed.testtype.InstrumentationTest;
import com.android.tradefed.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Power runner that runs an instrumentation test package on given device in a disconnected mode.
 * Stores instrumentation status output into proto file on device. After execution pulls proto file
 * from device and parses output from proto file, runs collectors and post processes.
 */
@OptionClass(alias = "power-runner")
public class PowerRunner extends InstrumentationTest {

    protected ITestInvocationListener mListener;
    protected TestInformation mTestInfo;
    private String mSDcardPath = null;
    private String mRunName = "PowerTest";

    public static final String INSTRUMENTATION_RESULTS_FILE_PATH = "protos";

    @Option(
            name = "power-instrumentation-arg",
            description = "Additional instrumentation arguments to provide.",
            requiredForRerun = true)
    private final Map<String, String> mPowerInstrArgMap = new HashMap<String, String>();

    @Option(
            name = "max-wait-time-for-device-to-be-offline",
            description =
                    "Maximum time a host should wait for a device to become offline after"
                            + " instrumentation command is run in nohup mode")
    private long mMaxWaitTimeForDeviceToBeOffline = 120000;

    @Option(
            name = "max-wait-time-for-device-to-be-online",
            description =
                    "This is the max timeout, the host will wait for the device to "
                            + "finish the test and reconnect. The device may connect back to host"
                            + " before this max time")
    private long mMaxWaitTimeForDeviceToBeOnline = 480000;

    /** {@inheritDoc} */
    @Override
    public void run(TestInformation testInfo, ITestInvocationListener listener)
            throws DeviceNotAvailableException {
        LogUtil.CLog.i("Starting run method of " + this.getClass().getSimpleName());
        if (getDevice() == null) {
            throw new IllegalArgumentException("Device has not been set");
        }
        setUp();

        mListener = listener;
        mTestInfo = testInfo;
        String makeInstrDirCommand = String.format("mkdir %s", INSTRUMENTATION_RESULTS_FILE_PATH);
        getDevice().executeShellCommand(makeInstrDirCommand);
        String instrCmd = prepareInstrumentationCommand();
        LogUtil.CLog.i("Command to run test in nohup mode prepared: " + instrCmd);
        NohupCommandHelper.executeAdbNohupCommand(getDevice(), instrCmd);

        waitForDeviceToBeDisconnected();
        // In the meantime test will run in disconnected mode
        waitForDeviceToBeConnected();

        // stop cable_breaker watchdog after the test run
        getDevice().executeShellCommand("cable_breaker -a end -w -");
        LogUtil.CLog.i("Cable Breaker watchdog turned off");
        parseInstrumentationResults();
    }

    /** Waits for the device to be disconnected from host. */
    public void waitForDeviceToBeDisconnected() {
        LogUtil.CLog.i(
                "Waiting for device "
                        + getDevice().getIDevice().getSerialNumber()
                        + " to be disconnected for "
                        + mMaxWaitTimeForDeviceToBeOffline
                        + " ms");

        // wait for device to be disconnected
        getDevice().waitForDeviceNotAvailable(mMaxWaitTimeForDeviceToBeOffline);
        if (getDevice().getIDevice().isOnline()) {
            String message =
                    "Device "
                            + getDevice().getIDevice().getSerialNumber()
                            + " not disconnected from host after waiting for "
                            + mMaxWaitTimeForDeviceToBeOffline;
            LogUtil.CLog.e(message);
            throw new RuntimeException(message);
        }
        LogUtil.CLog.i("Device " + getDevice().getIDevice().getSerialNumber() + " disconnected");
    }

    /** Waits for the device to connect back to host */
    public void waitForDeviceToBeConnected() throws DeviceNotAvailableException {
        LogUtil.CLog.i(
                "Waiting for device "
                        + getDevice().getIDevice().getSerialNumber()
                        + " to connect back to host, max wait time is "
                        + mMaxWaitTimeForDeviceToBeOnline
                        + " ms");
        getDevice().waitForDeviceOnline(mMaxWaitTimeForDeviceToBeOnline);
        if (getDevice().getIDevice().isOffline()) {
            String message =
                    "Device "
                            + getDevice().getIDevice().getSerialNumber()
                            + " not connected back to host after waiting for "
                            + mMaxWaitTimeForDeviceToBeOnline;
            LogUtil.CLog.e(message);
            throw new RuntimeException(message);
        }
        LogUtil.CLog.i(
                "Device "
                        + getDevice().getIDevice().getSerialNumber()
                        + " connected back to host after test completion");
    }

    private String prepareInstrumentationCommand() {
        List<String> command = new ArrayList<String>();
        command.add("am instrument -w -r");
        command.add("-f " + INSTRUMENTATION_RESULTS_FILE_PATH + "/output.proto --no-logcat");
        command.add("-w -r -e class");
        command.add(getClassName());
        for (Map.Entry<String, String> argEntry : mPowerInstrArgMap.entrySet()) {
            command.add("-e");
            command.add(argEntry.getKey());
            command.add(argEntry.getValue());
        }
        // cable_breaker accepts timeout in secs, hence convert ms to secs
        long timeToKeepDeviceDisconnected = (mMaxWaitTimeForDeviceToBeOffline / 1000);
        command.add("-e time-to-keep-device-disconnected");
        command.add(String.valueOf(timeToKeepDeviceDisconnected));

        command.add(getPackageName() + "/" + getRunnerName());

        // concatenate command tokens with spaces in between them
        String builtCommand = String.join(" ", command);
        return builtCommand;
    }

    /**
     * Parse the instrumentation proto output and invoke the host side listeners for further
     * collection and post-processing.
     *
     * @throws DeviceNotAvailableException
     */
    public void parseInstrumentationResults() throws DeviceNotAvailableException {
        File tmpDestDir = null;
        try {
            try {
                tmpDestDir = FileUtil.createTempDir("power-tests-tmp-results");
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to create the local folder in the host"
                                + " to store the instrumentation results.");
            }
            mSDcardPath = getDevice().getMountPoint(IDevice.MNT_EXTERNAL_STORAGE);
            if (getDevice()
                    .pullDir(
                            String.format("%s/%s", mSDcardPath, INSTRUMENTATION_RESULTS_FILE_PATH),
                            tmpDestDir)) {
                File[] files = tmpDestDir.listFiles();
                if (files.length == 0) {
                    throw new RuntimeException(
                            String.format(
                                    "Instrumentation results proto file not found under"
                                            + " %s/%s in the device",
                                    mSDcardPath, INSTRUMENTATION_RESULTS_FILE_PATH));
                }
                if (files.length > 1) {
                    throw new RuntimeException(
                            "More than one instrumentation result proto file found.");
                }

                parseProtoFile(mRunName, files[0]);
            }
        } finally {
            FileUtil.recursiveDelete(tmpDestDir);
        }
    }

    public void parseProtoFile(String runName, File file) {
        TestRunToTestInvocationForwarder runToInvocation =
                new TestRunToTestInvocationForwarder(mListener);
        List<ITestRunListener> runListeners = Collections.singletonList(runToInvocation);
        InstrumentationResultProtoParser protoParser =
                new InstrumentationResultProtoParser(runName, runListeners);
        try {
            protoParser.processProtoFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Unable to process the instrumentation proto file.");
        }
    }

    private void deleteTestFile(String filePath) throws DeviceNotAvailableException {
        if (getDevice().doesFileExist(filePath)) {
            getDevice().executeShellCommand(String.format("rm -rf %s", filePath));
        }
    }

    protected void deleteTestFiles() throws DeviceNotAvailableException {
        deleteTestFile(String.format("%s/%s", mSDcardPath, NohupCommandHelper.NOHUP_LOG));
        deleteTestFile(String.format("%s/%s", mSDcardPath, INSTRUMENTATION_RESULTS_FILE_PATH));
    }

    public void setUp() throws DeviceNotAvailableException {
        mSDcardPath = getDevice().getMountPoint(IDevice.MNT_EXTERNAL_STORAGE);
        // Clean previous test files if exist
        deleteTestFiles();
        LogUtil.CLog.i("Deleted existing test files on device if existed");
    }
}

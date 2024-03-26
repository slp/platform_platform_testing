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

package com.android.runner.utils;

import com.android.ddmlib.IDevice;
import com.android.tradefed.device.CollectingOutputReceiver;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;

import java.util.concurrent.TimeUnit;
import java.io.File;

/**
 * Helper class to execute async adb commands using nohup.
 *
 * <p>
 *
 * <p>If a process needs to keep running even after USB is disconnected use this helper.
 */
public class NohupCommandHelper {

    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 0;
    public static final String NOHUP_LOG = "nohup.log";

    private static final int DEFAULT_TIMEOUT = 120; // 2 minutes

    /** Helper method to execute adb command with nohup */
    public static void executeAdbNohupCommand(ITestDevice device, String cmd, int timeout)
            throws DeviceNotAvailableException {

        String logPath =
                String.format(
                        "%s/%s", device.getMountPoint(IDevice.MNT_EXTERNAL_STORAGE), NOHUP_LOG);
        File out = new File(logPath);
        if (!device.doesFileExist(out.getParent())) {
            throw new IllegalArgumentException("Output log's directory doesn't exist.");
        }

        StringBuilder builder = new StringBuilder();

        builder.append("nohup");
        builder.append(" ");
        builder.append(cmd);
        builder.append(" ");

        // Re-route stdout to a log.
        builder.append(String.format(">> %s", logPath));
        builder.append(" ");

        // Re-route errors to stdout.
        builder.append("2>&1");

        String finalCommand = builder.toString();

        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    CLog.d(
                                            "About to run async command on device %s: %s",
                                            device.getSerialNumber(), finalCommand);

                                    device.executeShellCommand(
                                            finalCommand,
                                            /* doing nothing with the output */
                                            new CollectingOutputReceiver(),
                                            timeout,
                                            TimeUnit.SECONDS,
                                            DEFAULT_MAX_RETRY_ATTEMPTS);
                                } catch (DeviceNotAvailableException e) {
                                    CLog.e(
                                            "Device became not available while running: %s",
                                            finalCommand);
                                    CLog.e(e);
                                }
                            }
                        })
                .start();
    }

    public static void executeAdbNohupCommand(ITestDevice device, String cmd)
            throws DeviceNotAvailableException {
        executeAdbNohupCommand(device, cmd, DEFAULT_TIMEOUT);
    }
}

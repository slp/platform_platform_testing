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

package android.device.collectors;

import android.content.Context;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.listener.InstrumentationRunListener;

import org.junit.runner.Description;

import java.io.IOException;

public class PowerTestRunListener extends InstrumentationRunListener {

    public static final String TAG = PowerTestRunListener.class.getCanonicalName();

    @Override
    public void testRunStarted(Description description) throws IOException {
        long maxTime =
                Long.valueOf(
                        InstrumentationRegistry.getArguments()
                                .getString("time-to-keep-device-disconnected"));
        Log.d(TAG, "Disconnecting device for " + maxTime + "second(s)");
        disconnectDeviceFromHost(maxTime);
        SystemClock.sleep(5000);
        if (!isCharging()) {
            Log.i(TAG, "Device disconnected successfully before the test start");
        }
        Log.i(TAG, "Finished testRunStarted");
    }

    private void disconnectDeviceFromHost(long maxTimeout) throws IOException {
        Log.i(TAG, "====== Entered in device disconnect from host method =====");
        String cbHelpCmd = "cable_breaker -h";
        Log.i(TAG, "Checking if cable breaker is present in the device using " + cbHelpCmd);
        getInstrumentation().getUiAutomation().executeShellCommand(cbHelpCmd);
        String command = "cable_breaker -a break:" + maxTimeout + " -w -";
        Log.i(TAG, "Running command to disconnect using cable breaker: " + command);
        getInstrumentation().getUiAutomation().executeShellCommand(command);
    }

    /*    private void connectDeviceToHost(){
        // TODO
        // Add implementation to connect device to host once the cable_breaker supports the
        // functionality to disconnect.
    }*/

    /** Checks whether device battery is charging */
    private boolean isCharging() {
        BatteryManager batteryManager =
                (BatteryManager)
                        getInstrumentation().getContext().getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.isCharging();
    }
}

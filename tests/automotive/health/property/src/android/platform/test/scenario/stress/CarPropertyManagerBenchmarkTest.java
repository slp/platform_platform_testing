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
package android.platform.test.scenario.stress;

import static android.car.VehiclePropertyIds.HVAC_TEMPERATURE_SET;
import static android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.car.Car;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.device.loggers.TestLogData;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class CarPropertyManagerBenchmarkTest {
    private static final String TAG = CarPropertyManagerBenchmarkTest.class.getSimpleName();
    private static final int NUM_TASKS = 30;
    private static final int NUM_RUNS = 30;
    private static final int MIN_TEMP = 16;
    private static final int MAX_TEMP = 25;
    private CarPropertyManager mCarPropertyManager;
    private Context mContext;
    @Rule public TestLogData logs = new TestLogData();

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        Car car = Car.createCar(mContext);
        assertThat(car).isNotNull();
        mCarPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
        assertThat(mCarPropertyManager).isNotNull();
    }

    @Test
    public void testGetPropertyStress() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_TASKS);
        final CountDownLatch latch = new CountDownLatch(NUM_TASKS); // For synchronization
        ConcurrentLinkedQueue<Long> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < NUM_TASKS; i++) {
            executorService.execute(
                    () -> {
                        for (int j = 0; j < NUM_RUNS; j++) {
                            long timeBeforeRun = System.nanoTime();
                            try {
                                mCarPropertyManager.getProperty(PERF_VEHICLE_SPEED, 0);
                            } catch (IllegalStateException e) {
                                // This is expected because car service only allows up to 16 sync
                                // get/set operations happening at the same time.
                                Log.i(TAG, "getProperty threw an error", e);
                            }
                            concurrentLinkedQueue.add((System.nanoTime() - timeBeforeRun));
                        }
                        latch.countDown();
                    });
        }
        executorService.shutdown();
        latch.await(10, TimeUnit.SECONDS);

        File fileToLog = writeToFile("values_for_test_get", concurrentLinkedQueue);
        if (fileToLog != null) {
            logs.addTestLog("getPropertySync_latency_in_nanos", fileToLog);
        }
    }

    @Test
    public void testSetPropertyStress() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_TASKS);
        final CountDownLatch latch = new CountDownLatch(NUM_TASKS); // For synchronization
        ConcurrentLinkedQueue<Long> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        CarPropertyConfig<?> hvacTempSetConfig =
                mCarPropertyManager.getCarPropertyConfig(HVAC_TEMPERATURE_SET);
        assumeTrue("Cannot set hvacTemp", hvacTempSetConfig != null);
        int areaId = hvacTempSetConfig.getAreaIdConfigs().get(0).getAreaId();
        for (int i = 0; i < NUM_TASKS; i++) {
            executorService.execute(
                    () -> {
                        for (int j = 0; j < NUM_RUNS; j++) {
                            Random rd = new Random();
                            long timeBeforeRun = System.nanoTime();
                            try {
                                mCarPropertyManager.setProperty(
                                        Float.class,
                                        HVAC_TEMPERATURE_SET,
                                        /* areaId= */ areaId,
                                        /* val= */ MIN_TEMP
                                                + rd.nextFloat() * (MAX_TEMP - MIN_TEMP));
                            } catch (IllegalStateException e) {
                                // This is expected because car service only allows up to 16 sync
                                // get/set operations happening at the same time.
                                Log.i(TAG, "getProperty threw an error", e);
                            }
                            concurrentLinkedQueue.add((System.nanoTime() - timeBeforeRun));
                        }
                        latch.countDown();
                    });
        }
        executorService.shutdown();
        latch.await(10, TimeUnit.SECONDS);

        File fileToLog = writeToFile("values_for_test_set", concurrentLinkedQueue);
        if (fileToLog != null) {
            logs.addTestLog("setPropertySync_latency_in_nanos", fileToLog);
        }
    }

    private File writeToFile(String fileName, ConcurrentLinkedQueue<Long> valuesToWrite)
            throws Exception {
        File file = new File(mContext.getFilesDir() + "/" + fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        try (FileWriter writer = new FileWriter(file, true)) {
            for (long valueToWrite : valuesToWrite) {
                writer.write(String.valueOf(valueToWrite));
                writer.write(System.lineSeparator());
            }
        }
        return file;
    }
}

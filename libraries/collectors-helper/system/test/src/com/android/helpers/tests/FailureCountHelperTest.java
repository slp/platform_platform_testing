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
package com.android.helpers.tests;

import static org.junit.Assert.assertEquals;

import androidx.test.runner.AndroidJUnit4;

import com.android.helpers.FailureCountHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

/**
 * Android Unit tests for {@link FailureCountHelper}.
 *
 * <p>atest CollectorsHelperTest:com.android.helpers.FailureCountHelperTest
 */
@RunWith(AndroidJUnit4.class)
public class FailureCountHelperTest {

    private static final String TAG = FailureCountHelperTest.class.getSimpleName();
    private static final double DELTA = 0.00001;

    private FailureCountHelper mHelper;

    @Before
    public void setUp() throws IOException {
        mHelper = new FailureCountHelper();
        mHelper.startCollecting();
    }

    @After
    public void tearDown() {
        // stopCollecting() is currently a no-op but is included here in case it is updated.
        mHelper.stopCollecting();
    }

    @Test
    public void testGetMetricsWithFailure() {
        Description testDesc1 =
                Description.createTestDescription(
                        "android.platform.test.scenario.youtube.PlayVideo$12", "testPlayVideo");
        Description testDesc2 =
                Description.createTestDescription(
                        "android.platform.test.scenario.youtube.PlayVideo$15", "testPlayVideo");
        Description testDesc3 =
                Description.createTestDescription(
                        "android.platform.test.scenario.system.RotateScreen$5", "testRotateScreen");
        mHelper.recordFailure(testDesc1);
        mHelper.recordFailure(testDesc2);
        mHelper.recordFailure(testDesc3);
        Map<String, Integer> metrics = mHelper.getMetrics();
        assertEquals(2, metrics.size(), DELTA);
        assertEquals(2, metrics.get(mHelper.convertTestDescriptionToKey(testDesc1)), DELTA);
        assertEquals(1, metrics.get(mHelper.convertTestDescriptionToKey(testDesc3)), DELTA);
    }

    @Test
    public void testConvertTestDisplayNameToKey() {
        Description testDesc1 =
                Description.createTestDescription(
                        "android.platform.test.scenario.youtube.PlayVideo$12", "testPlayVideo");
        Description testDesc2 =
                Description.createTestDescription(
                        "android.platform.test.scenario.youtube.PlayVideo", "testPlayVideo");

        String expectedKey = "android.platform.test.scenario.youtube.PlayVideo.testPlayVideo";
        assertEquals(expectedKey, mHelper.convertTestDescriptionToKey(testDesc1));
        assertEquals(expectedKey, mHelper.convertTestDescriptionToKey(testDesc2));
    }
}

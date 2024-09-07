/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import com.android.helpers.PinnerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Map;

/**
 * Android Unit tests for {@link PinnerHelper}.
 *
 * To run:
 * atest CollectorsHelperTest:com.android.helpers.tests.PinnerHelperTest
 */
@RunWith(AndroidJUnit4.class)
public class PinnerHelperTest {
    private static final String TAG = PinnerHelperTest.class.getSimpleName();
    // Valid output file
    private static final String VALID_OUTPUT_DIR = "/sdcard/test_results";
    // Invalid output file (no permissions to write)
    private static final String INVALID_OUTPUT_DIR = "/data/local/tmp";

    private PinnerHelper mPinnerHelper;

    @Before
    public void setUp() {
        mPinnerHelper = new PinnerHelper();
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws IOException {
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.executeShellCommand(String.format("rm -rf %s", VALID_OUTPUT_DIR));
    }

    /** Test start collecting returns false if the helper has not been properly set up. */
    @Test
    public void testSetUpNotCalled() {
        assertFalse(mPinnerHelper.startCollecting());
    }

    /** Test writing file to a valid output directory. */
    @Test
    public void testValidFile() {
        mPinnerHelper.setUp(VALID_OUTPUT_DIR);
        assertTrue(mPinnerHelper.startCollecting());
    }

    /** Test writing file to a invalid directory. */
    @Test
    public void testInvalidFile() {
        mPinnerHelper.setUp(INVALID_OUTPUT_DIR);
        assertFalse(mPinnerHelper.startCollecting());
    }

    /** Test the pinner snapshot file is added in the result map. */
    @Test
    public void testPinnerSnapshotDumpFile() {
        mPinnerHelper.setUp(VALID_OUTPUT_DIR);
        assertTrue(mPinnerHelper.startCollecting());
        Map<String, String> metrics = mPinnerHelper.getMetrics();
        assertTrue(metrics.size() > 2);
        assertTrue(metrics.containsKey(PinnerHelper.OUTPUT_FILE_PATH_KEY));
    }

    /**
     * Test the total pinned files size is added in the result map.
     */
    @Test
    public void testPinnerTotalFileSizeMetric() {
        mPinnerHelper.setUp(VALID_OUTPUT_DIR);
        assertTrue(mPinnerHelper.startCollecting());
        Map<String, String> metrics = mPinnerHelper.getMetrics();
        assertTrue(metrics.size() > 2);
        assertTrue(metrics.containsKey(PinnerHelper.TOTAL_SIZE_BYTES_KEY));
    }

    /**
     * Test total file size count is sum of individual app level and system file size counts.
     */
    @Test
    public void testValidateTotalFileSizeCount() {
        mPinnerHelper.setUp(VALID_OUTPUT_DIR);
        assertTrue(mPinnerHelper.startCollecting());
        Map<String, String> metrics = mPinnerHelper.getMetrics();
        int totalFilesCount = metrics.entrySet().stream().filter(
                s -> (s.getKey().endsWith(PinnerHelper.PINNER_FILES_COUNT_SUFFIX) && !s.getKey()
                        .startsWith(PinnerHelper.TOTAL_FILE_COUNT_KEY)))
                .map(e -> e.getValue()).mapToInt(Integer::valueOf).sum();
        assertTrue(
                totalFilesCount == Integer
                        .parseInt(metrics.get(PinnerHelper.TOTAL_FILE_COUNT_KEY)));
    }
}

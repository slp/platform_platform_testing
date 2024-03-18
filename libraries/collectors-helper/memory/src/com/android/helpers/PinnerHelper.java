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

package com.android.helpers;

import android.app.pinner.PinnedFileStat;
import android.app.pinner.PinnerServiceClient;
import android.util.Log;

import androidx.test.uiautomator.UiDevice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper to collect pinned files information from the device using dumpsys.
 */
public class PinnerHelper implements ICollectorHelper<String> {
    private static final String TAG = PinnerHelper.class.getSimpleName();
    public static final String PINNER_CMD = "dumpsys pinner";
  public static final String SYSTEM_HEADER_NAME = "system";
  public static final String TOTAL_SIZE_BYTES_KEY = "pinner_total_size_bytes";
  public static final String TOTAL_FILE_COUNT_KEY = "pinner_total_files_count";
    public static final String OUTPUT_FILE_PATH_KEY = "pinner_output_file";
  public static final String PINNER_FILES_COUNT_SUFFIX = "files_count";
    private String mTestOutputDir = null;
    private String mTestOutputFile = null;

  // Map to maintain pinned files memory usage.
  private Map<String, String> mPinnerMap = new HashMap<>();

    public void setUp(String testOutputDir) {
        mTestOutputDir = testOutputDir;
    }

  @Override
  public boolean startCollecting() {
        if (mTestOutputDir == null) {
            Log.e(TAG, String.format("Invalid test setup"));
            return false;
        }

        File directory = new File(mTestOutputDir);
        String filePath =
                String.format(
                        "%s/pinner_snapshot%d.txt", mTestOutputDir, UUID.randomUUID().hashCode());
        File file = new File(filePath);

        // Make sure directory exists and file does not
        if (directory.exists()) {
            if (file.exists() && !file.delete()) {
                Log.e(
                        TAG,
                        String.format(
                                "Result file %s already exists and cannot be deleted", filePath));
                return false;
            }
        } else {
            if (!directory.mkdirs()) {
                Log.e(
                        TAG,
                        String.format(
                                "Failed to create result output directory %s", mTestOutputDir));
                return false;
            }
        }

        // Create an empty file to fail early in case there are no write permissions
        try {
            if (!file.createNewFile()) {
                // This should not happen unless someone created the file right after we deleted it
                Log.e(
                        TAG,
                        String.format("Race with another user of result output file %s", filePath));
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, String.format("Failed to create result output file %s", filePath), e);
            return false;
        }

        mTestOutputFile = filePath;
    return true;
  }

  @Override
  public Map<String, String> getMetrics() {
    mPinnerMap = new HashMap<>();

    PinnerServiceClient pinnerClient = new PinnerServiceClient();
    List<PinnedFileStat> stats = pinnerClient.getPinnerStats();

    // Parse the per file memory usage and files count from the pinner details.
    updatePinnerInfo(stats);

        // Get dumpsys pinner output for debugging
        String pinnerOutput;
        try {
            pinnerOutput = executeShellCommand(PINNER_CMD);
            Log.i(TAG, "Pinner output:\n" + pinnerOutput);
        } catch (IOException e) {
            throw new RuntimeException("Unable to execute dumpsys pinner command", e);
        }

        // Write the pinner output to a file and update the output metrics with the
        // path to the file.
        if (mTestOutputFile != null) {
            try {
                FileWriter writer = new FileWriter(new File(mTestOutputFile), true);
                storeToFile(mTestOutputFile, pinnerOutput, writer);
                writer.close();
                mPinnerMap.put(OUTPUT_FILE_PATH_KEY, mTestOutputFile);
            } catch (IOException e) {
                Log.e(TAG, String.format("Failed to write output file %s", mTestOutputFile), e);
            }
        }

    return mPinnerMap;
  }

  @Override
  public boolean stopCollecting() {
    return true;
  }

  private void updatePinnerInfo(List<PinnedFileStat> stats) {
    int totalFilesCount = 0;
    int totalBytes = 0;
    HashSet<String> groups = new HashSet<>();
    for (PinnedFileStat stat : stats) {
      // individual pinned file sizes.
      mPinnerMap.put(
          String.format("pinner_%s_%s_bytes", stat.getGroupName(), stat.getFilename()),
          String.valueOf(stat.getBytesPinned()));
      totalBytes += stat.getBytesPinned();
      totalFilesCount++;
      if (!groups.contains(stat.getGroupName())) {
        groups.add(stat.getGroupName());
      }
    }
    for (String group : groups) {
      long filesInGroup =
          stats.stream().filter(f -> f.getGroupName().equals(group)).count();
      String groupInMetric = group;
      if (groupInMetric.equals("")) {
        // Default group will be system
        groupInMetric = SYSTEM_HEADER_NAME;
      }
      mPinnerMap.put(
          String.format("pinner_%s_%s", groupInMetric, PINNER_FILES_COUNT_SUFFIX),
          String.valueOf(filesInGroup));
    }

    // Update the previous app pinned file count.
    mPinnerMap.put(TOTAL_FILE_COUNT_KEY, String.valueOf(totalFilesCount));
    mPinnerMap.put(TOTAL_SIZE_BYTES_KEY, String.valueOf(totalBytes));
  }

  /* Execute a shell command and return its output. */
  public String executeShellCommand(String command) throws IOException {
    return UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        .executeShellCommand(command);
  }

    // Store dumpsys raw output in a text file.
    private void storeToFile(String fileName, String pinnerOutput, FileWriter writer)
            throws RuntimeException {
        try {
            writer.write("Pinned files details.");
            writer.write(pinnerOutput);
            writer.write('\n');
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to write file %s ", fileName), e);
        }
    }
}

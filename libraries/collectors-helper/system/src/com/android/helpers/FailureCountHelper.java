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

package com.android.helpers;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.junit.runner.Description;

import java.util.HashMap;
import java.util.Map;

/** FailureCountHelper is used to collect the number of times a test case fails. */
public class FailureCountHelper implements ICollectorHelper<Integer> {

    private static final String TAG = FailureCountHelper.class.getSimpleName();
    private Map<String, Integer> mMetrics;

    public FailureCountHelper() {
        super();
        mMetrics = new HashMap<>();
    }

    @Override
    public boolean startCollecting() {
        Log.d(TAG, "Started collecting for FailureCount.");
        return true;
    }

    @Override
    public Map<String, Integer> getMetrics() {
        Log.d(TAG, "Grabbing mMetrics for FailureCount.");
        return mMetrics;
    }

    @Override
    public boolean stopCollecting() {
        Log.d(TAG, "Stopped collecting for FailureCount.");
        return true;
    }

    /** Method for recording the name of a failed test case as a metric. */
    @VisibleForTesting
    public void recordFailure(Description description) {
        String key = convertTestDescriptionToKey(description);
        mMetrics.putIfAbsent(key, 0);
        mMetrics.put(key, mMetrics.get(key) + 1);
    }

    /** Converts test descriptions to metric keys. */
    @VisibleForTesting
    public String convertTestDescriptionToKey(Description description) {
        String testClass = description.getClassName();
        String testMethod = description.getMethodName();
        // The iteration count must be removed if it exists.
        return String.format(
                "failure_count_%s.%s", testClass.split("\\$")[0], testMethod.split("\\$")[0]);
    }
}

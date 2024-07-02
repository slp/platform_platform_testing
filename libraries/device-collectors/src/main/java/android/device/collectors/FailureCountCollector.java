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

import android.device.collectors.annotations.OptionClass;

import com.android.helpers.FailureCountHelper;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

/** A {link FailureCountCollector} captures the number of times a test case fails in a test run. */
@OptionClass(alias = "failure-count-collector")
public class FailureCountCollector extends BaseCollectionListener<Integer> {

    private FailureCountHelper mHelper;

    public FailureCountCollector() {
        // We hold on to the FailureCountHelper because this collector has methods that fire on test
        // failures, but the helper is responsible for computing and returning metrics.
        mHelper = new FailureCountHelper();
        createHelperInstance(mHelper);
    }

    @Override
    public void onTestFail(DataRecord testData, Description description, Failure failure) {
        mHelper.recordFailure(description);
        super.onTestFail(testData, description, failure);
    }
}

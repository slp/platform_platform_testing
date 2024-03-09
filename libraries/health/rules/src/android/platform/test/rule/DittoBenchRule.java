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
package android.platform.test.rule;

import androidx.annotation.VisibleForTesting;

import org.junit.runner.Description;

/** This rule will start dittobench using the provided config. */
public class DittoBenchRule extends TestWatcher {
    @VisibleForTesting static final String DITTOBENCH_PATH = "dittobench-path";
    @VisibleForTesting static final String DITTO_CONFIG_PATH = "ditto-config-path";
    String mDittoBenchPath = "/data/local/tmp/dittobench";
    String mDittoConfigPath = "/data/local/tmp/config.ditto";

    @Override
    protected void starting(Description description) {
        mDittoBenchPath = getArguments().getString(DITTOBENCH_PATH, "/data/local/tmp/dittobench");
        mDittoConfigPath =
                getArguments().getString(DITTO_CONFIG_PATH, "/data/local/tmp/config.ditto");
        executeShellCommand(mDittoBenchPath + " " + mDittoConfigPath);
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        String[] splitPath = mDittoBenchPath.split("/");
        if (splitPath != null && splitPath.length != 0) {
            String dittoBinaryName = splitPath[splitPath.length - 1];
            executeShellCommand("pkill " + dittoBinaryName);
        }
    }
}
